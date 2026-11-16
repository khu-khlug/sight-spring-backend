package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupSearchKeywords
import com.sight.domain.group.GroupSearchPreview
import com.sight.repository.GroupRepository
import com.sight.repository.GroupSearchRepository
import com.sight.repository.dto.GroupSearchCardView
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class GroupSearchItem(
    val listId: Long,
    val listTitle: String,
    val cardId: Long,
    val cardTitle: String,
    val recordId: Long?,
    val matchedText: String?,
)

data class GroupSearchResult(
    val page: Int,
    val totalPages: Long,
    val results: List<GroupSearchItem>,
)

@Service
class GroupSearchService(
    private val groupRepository: GroupRepository,
    private val groupSearchRepository: GroupSearchRepository,
    private val groupRecordTextExtractor: GroupRecordTextExtractor,
) {
    @Transactional(readOnly = true)
    fun listGroupSearchResults(
        groupId: Long,
        target: GroupSearchTarget,
        keyword: String,
        page: Int,
    ): GroupSearchResult {
        if (page < 1) throw BadRequestException("페이지 번호는 1 이상이어야 합니다")
        val keywords =
            try {
                GroupSearchKeywords.parse(keyword)
            } catch (e: IllegalArgumentException) {
                throw BadRequestException(e.message ?: "검색어가 올바르지 않습니다")
            }
        if (!groupRepository.existsById(groupId)) {
            throw NotFoundException("그룹을 찾을 수 없습니다")
        }

        val offset = (page - 1).toLong() * PAGE_SIZE
        return when (target) {
            GroupSearchTarget.TITLE ->
                GroupSearchResult(
                    page = page,
                    totalPages = totalPages(groupSearchRepository.countCardsByTitle(groupId, keywords)),
                    results =
                        groupSearchRepository
                            .findCardsByTitleOrderByLatest(groupId, keywords, offset, PAGE_SIZE)
                            .map { it.toItem(matchedText = null) },
                )
            GroupSearchTarget.AUTHOR ->
                GroupSearchResult(
                    page = page,
                    totalPages = totalPages(groupSearchRepository.countCardsByAuthorName(groupId, keywords)),
                    results =
                        groupSearchRepository
                            .findCardsByAuthorNameOrderByLatest(groupId, keywords, offset, PAGE_SIZE)
                            .map { it.toItem(matchedText = it.authorName) },
                )
            GroupSearchTarget.RECORD -> searchRecords(groupId, keywords, page, offset)
        }
    }

    // 기록 본문은 저장 형식(HTML·JSON)에서 표시 텍스트를 추출해야 일치 여부를 알 수 있다.
    // 최근 변경순으로 배치 단위로 읽으며 일치 건수를 세고, 요청한 페이지에 해당하는 결과만 미리보기와 함께 보관한다.
    private fun searchRecords(
        groupId: Long,
        keywords: GroupSearchKeywords,
        page: Int,
        offset: Long,
    ): GroupSearchResult {
        data class RecordHit(
            val recordId: Long,
            val cardId: Long,
            val preview: String,
        )

        val hits = mutableListOf<RecordHit>()
        var matchedCount = 0L
        var scanned = 0L
        while (true) {
            val batch = groupSearchRepository.findSearchableRecordsOrderByLatest(groupId, scanned, SCAN_BATCH_SIZE)
            batch.forEach { record ->
                val text = groupRecordTextExtractor.extract(record.type, record.content)
                if (!keywords.matches(text)) return@forEach
                if (matchedCount >= offset && hits.size < PAGE_SIZE) {
                    hits += RecordHit(record.recordId, record.cardId, GroupSearchPreview.create(text, keywords))
                }
                matchedCount++
            }
            scanned += batch.size
            if (batch.size < SCAN_BATCH_SIZE) break
        }

        val cards = groupSearchRepository.findCardViewsByIds(groupId, hits.map { it.cardId }.toSet()).associateBy { it.cardId }
        return GroupSearchResult(
            page = page,
            totalPages = totalPages(matchedCount),
            results =
                hits.mapNotNull { hit ->
                    cards[hit.cardId]?.let { card ->
                        GroupSearchItem(
                            listId = card.listId,
                            listTitle = card.listTitle,
                            cardId = card.cardId,
                            cardTitle = card.cardTitle,
                            recordId = hit.recordId,
                            matchedText = hit.preview,
                        )
                    }
                },
        )
    }

    private fun totalPages(totalCount: Long): Long = (totalCount + PAGE_SIZE - 1) / PAGE_SIZE

    private fun GroupSearchCardView.toItem(matchedText: String?): GroupSearchItem =
        GroupSearchItem(
            listId = listId,
            listTitle = listTitle,
            cardId = cardId,
            cardTitle = cardTitle,
            recordId = null,
            matchedText = matchedText,
        )

    companion object {
        // 한 페이지에 반환하는 결과 수.
        const val PAGE_SIZE: Int = 50

        // 기록 본문 검색 시 한 번에 읽는 기록 수.
        const val SCAN_BATCH_SIZE: Int = 50

        // 검색어 최대 길이.
        const val KEYWORD_MAX_LENGTH: Int = GroupSearchKeywords.MAX_LENGTH
    }
}
