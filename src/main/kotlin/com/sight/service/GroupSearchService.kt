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
        // 대상과 관계없이 같은 비교 규칙(GroupSearchKeywords)으로 일치 여부를 판정하고, 결과는 한 곳에서 만든다.
        val outcome =
            when (target) {
                GroupSearchTarget.TITLE -> searchCards(groupId, keywords, offset, { it.cardTitle }, { null })
                GroupSearchTarget.AUTHOR -> searchCards(groupId, keywords, offset, { it.authorName }, { it.authorName })
                GroupSearchTarget.RECORD -> searchRecords(groupId, keywords, offset)
            }
        return GroupSearchResult(page = page, totalPages = totalPages(outcome.matchedCount), results = outcome.items)
    }

    // 카드명·생성자명 검색. `textOf`가 일치 여부를 판정할 텍스트를, `matchedTextOf`가 결과에 담을 일치 텍스트를 정한다.
    private fun searchCards(
        groupId: Long,
        keywords: GroupSearchKeywords,
        offset: Long,
        textOf: (GroupSearchCardView) -> String,
        matchedTextOf: (GroupSearchCardView) -> String?,
    ): SearchOutcome {
        val scan =
            scan(
                keywords = keywords,
                offset = offset,
                batchSize = CARD_SCAN_BATCH_SIZE,
                fetch = { groupSearchRepository.findCardsOrderByLatest(groupId, it, CARD_SCAN_BATCH_SIZE) },
                textOf = textOf,
            )
        return SearchOutcome(
            matchedCount = scan.matchedCount,
            items = scan.hits.map { (card, _) -> card.toItem(recordId = null, matchedText = matchedTextOf(card)) },
        )
    }

    // 기록 본문 검색. 저장 형식(HTML·JSON)에서 표시 텍스트를 추출해야 일치 여부를 알 수 있다.
    private fun searchRecords(
        groupId: Long,
        keywords: GroupSearchKeywords,
        offset: Long,
    ): SearchOutcome {
        val scan =
            scan(
                keywords = keywords,
                offset = offset,
                batchSize = RECORD_SCAN_BATCH_SIZE,
                fetch = { groupSearchRepository.findSearchableRecordsOrderByLatest(groupId, it, RECORD_SCAN_BATCH_SIZE) },
                textOf = { groupRecordTextExtractor.extract(it.type, it.content) },
            )
        val cards =
            groupSearchRepository
                .findCardViewsByIds(groupId, scan.hits.map { (record, _) -> record.cardId }.toSet())
                .associateBy { it.cardId }
        return SearchOutcome(
            matchedCount = scan.matchedCount,
            items =
                scan.hits.mapNotNull { (record, match) ->
                    cards[record.cardId]?.toItem(recordId = record.recordId, matchedText = GroupSearchPreview.create(match))
                },
        )
    }

    // 최근 변경순으로 배치 단위로 읽으며 일치 건수를 세고, 요청한 페이지에 해당하는 항목만 일치 정보와 함께 보관한다.
    private fun <T> scan(
        keywords: GroupSearchKeywords,
        offset: Long,
        batchSize: Int,
        fetch: (Long) -> List<T>,
        textOf: (T) -> String,
    ): ScanResult<T> {
        val hits = mutableListOf<Pair<T, GroupSearchKeywords.Match>>()
        var matchedCount = 0L
        var scanned = 0L
        while (true) {
            val batch = fetch(scanned)
            batch.forEach { item ->
                val match = keywords.find(textOf(item)) ?: return@forEach
                if (matchedCount >= offset && hits.size < PAGE_SIZE) hits += item to match
                matchedCount++
            }
            scanned += batch.size
            if (batch.size < batchSize) break
        }
        return ScanResult(matchedCount, hits)
    }

    private class ScanResult<T>(
        val matchedCount: Long,
        val hits: List<Pair<T, GroupSearchKeywords.Match>>,
    )

    private class SearchOutcome(
        val matchedCount: Long,
        val items: List<GroupSearchItem>,
    )

    private fun totalPages(totalCount: Long): Long = (totalCount + PAGE_SIZE - 1) / PAGE_SIZE

    private fun GroupSearchCardView.toItem(
        recordId: Long?,
        matchedText: String?,
    ): GroupSearchItem =
        GroupSearchItem(
            listId = listId,
            listTitle = listTitle,
            cardId = cardId,
            cardTitle = cardTitle,
            recordId = recordId,
            matchedText = matchedText,
        )

    companion object {
        // 한 페이지에 반환하는 결과 수.
        const val PAGE_SIZE: Int = 50

        // 카드명·생성자명 검색 시 한 번에 읽는 카드 수. 카드는 짧은 문자열만 읽는다.
        const val CARD_SCAN_BATCH_SIZE: Int = 200

        // 기록 본문 검색 시 한 번에 읽는 기록 수. 본문은 최대 1MB라 작게 읽는다.
        const val RECORD_SCAN_BATCH_SIZE: Int = 50
    }
}
