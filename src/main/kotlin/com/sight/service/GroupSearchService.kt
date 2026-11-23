package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupCard
import com.sight.domain.group.GroupSearchKeywords
import com.sight.domain.group.GroupSearchPreview
import com.sight.repository.GroupCardRepository
import com.sight.repository.GroupKanbanListRepository
import com.sight.repository.GroupRepository
import com.sight.repository.GroupSearchRepository
import com.sight.repository.MemberRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
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
    private val groupCardRepository: GroupCardRepository,
    private val groupKanbanListRepository: GroupKanbanListRepository,
    private val memberRepository: MemberRepository,
    private val groupSearchRepository: GroupSearchRepository,
    private val groupRecordTextExtractor: GroupRecordTextExtractor,
) {
    // 한 번의 검색이 오프셋을 키워 가며 여러 번 읽으므로, 서버 기본 격리 수준과 관계없이 첫 읽기 시점의 스냅샷으로 일관되게 읽는다.
    // 중간에 카드·기록이 바뀌어도 같은 항목이 두 번 나오거나 빠지지 않는다.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun listGroupSearchResults(
        groupId: Long,
        target: GroupSearchTarget,
        keyword: String,
        page: Int,
    ): GroupSearchResult {
        if (!groupRepository.existsById(groupId)) {
            throw NotFoundException("그룹을 찾을 수 없습니다")
        }
        val keywords =
            try {
                GroupSearchKeywords.parse(keyword)
            } catch (e: IllegalArgumentException) {
                throw BadRequestException(e.message ?: "검색어가 올바르지 않습니다")
            }

        val offset = (page - 1).toLong() * PAGE_SIZE
        // 대상과 관계없이 같은 비교 규칙(GroupSearchKeywords)으로 일치 여부를 판정하고, 결과는 한 곳에서 만든다.
        val outcome =
            when (target) {
                GroupSearchTarget.TITLE -> searchCards(groupId, keywords, offset, byAuthorName = false)
                GroupSearchTarget.AUTHOR -> searchCards(groupId, keywords, offset, byAuthorName = true)
                GroupSearchTarget.RECORD -> searchRecords(groupId, keywords, offset)
            }
        return GroupSearchResult(page = page, totalPages = totalPages(outcome.matchedCount), results = outcome.items)
    }

    // 카드명 또는 카드 생성자명 검색. 카드·리스트·회원을 각 Repository에서 객체로 읽어 이 서비스에서 조합한다.
    // 카드 엔티티는 영속성 컨텍스트에 트랜잭션이 끝날 때까지 남지만 짧은 필드만 가지므로 크기가 작다.
    private fun searchCards(
        groupId: Long,
        keywords: GroupSearchKeywords,
        offset: Long,
        byAuthorName: Boolean,
    ): SearchOutcome {
        val authorNames = mutableMapOf<Long, String>()
        val scan =
            scan(
                keywords = keywords,
                offset = offset,
                batchSize = CARD_SCAN_BATCH_SIZE,
                fetch = { start ->
                    fetchCards(groupId, start).also { if (byAuthorName) loadAuthorNames(it, authorNames) }
                },
                textOf = { card -> if (byAuthorName) authorNames[card.authorMemberId].orEmpty() else card.title },
                toHit = { card, _ -> card },
            )
        val listTitles = loadListTitles(scan.hits.mapNotNull { it.listId })
        return SearchOutcome(
            matchedCount = scan.matchedCount,
            items =
                scan.hits.mapNotNull { card ->
                    card.toItem(
                        listTitles = listTitles,
                        recordId = null,
                        matchedText = if (byAuthorName) authorNames[card.authorMemberId] else null,
                    )
                },
        )
    }

    // 기록 본문 검색. 저장 형식(HTML·JSON)에서 표시 텍스트를 추출해야 일치 여부를 알 수 있다.
    // 기록은 본문이 커서 엔티티로 받지 않고 View로 읽는다(GroupSearchRepository 참고).
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
                // 본문(최대 1MB)을 붙들지 않도록 일치한 시점에 미리보기만 만들어 보관한다.
                toHit = { record, match -> RecordHit(record.recordId, record.cardId, GroupSearchPreview.create(match)) },
            )
        val cards =
            groupCardRepository
                .findAllById(scan.hits.map { it.cardId }.toSet())
                .filter { it.groupId == groupId && !it.isDeleted }
                .associateBy { it.id }
        val listTitles = loadListTitles(cards.values.mapNotNull { it.listId })
        return SearchOutcome(
            matchedCount = scan.matchedCount,
            items =
                scan.hits.mapNotNull { hit ->
                    cards[hit.cardId]?.toItem(listTitles = listTitles, recordId = hit.recordId, matchedText = hit.preview)
                },
        )
    }

    // 스캔은 항상 배치 크기의 배수 위치에서 다음 배치를 읽으므로 오프셋은 페이지 번호로 정확히 환산된다.
    private fun fetchCards(
        groupId: Long,
        offset: Long,
    ): List<GroupCard> =
        groupCardRepository.findByGroupIdAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(
            groupId,
            PageRequest.of((offset / CARD_SCAN_BATCH_SIZE).toInt(), CARD_SCAN_BATCH_SIZE),
        )

    // 이미 읽은 회원은 다시 조회하지 않는다. 탈퇴해도 회원 행과 실명은 남는다.
    private fun loadAuthorNames(
        cards: List<GroupCard>,
        authorNames: MutableMap<Long, String>,
    ) {
        val missing = cards.map { it.authorMemberId }.filter { it !in authorNames }.toSet()
        if (missing.isEmpty()) return
        memberRepository.findAllById(missing).forEach { authorNames[it.id] = it.realname }
    }

    private fun loadListTitles(listIds: Collection<Long>): Map<Long, String> {
        val ids = listIds.toSet()
        if (ids.isEmpty()) return emptyMap()
        return groupKanbanListRepository.findAllById(ids).associate { it.id to it.title }
    }

    // 최근 변경순으로 배치 단위로 읽으며 일치 건수를 센다. 요청한 페이지에 해당하는 항목만 `toHit`으로 작은 값으로 바꿔 보관하고,
    // 읽은 배치와 일치 정보(정규화한 본문 전체)는 항목마다 바로 버려 메모리를 오래 쥐지 않는다.
    private fun <T, R> scan(
        keywords: GroupSearchKeywords,
        offset: Long,
        batchSize: Int,
        fetch: (Long) -> List<T>,
        textOf: (T) -> String,
        toHit: (T, GroupSearchKeywords.Match) -> R,
    ): ScanResult<R> {
        val hits = mutableListOf<R>()
        var matchedCount = 0L
        var scanned = 0L
        while (true) {
            val batch = fetch(scanned)
            batch.forEach { item ->
                val match = keywords.find(textOf(item)) ?: return@forEach
                if (matchedCount >= offset && hits.size < PAGE_SIZE) hits += toHit(item, match)
                matchedCount++
            }
            scanned += batch.size
            if (batch.size < batchSize) break
        }
        return ScanResult(matchedCount, hits)
    }

    private class ScanResult<R>(
        val matchedCount: Long,
        val hits: List<R>,
    )

    // 기록 검색의 일치 항목. 본문 대신 미리보기만 가진다.
    private class RecordHit(
        val recordId: Long,
        val cardId: Long,
        val preview: String,
    )

    private class SearchOutcome(
        val matchedCount: Long,
        val items: List<GroupSearchItem>,
    )

    private fun totalPages(totalCount: Long): Long = (totalCount + PAGE_SIZE - 1) / PAGE_SIZE

    // 삭제되지 않은 카드는 항상 리스트에 속한다. 리스트를 찾지 못하면 결과에서 뺀다.
    private fun GroupCard.toItem(
        listTitles: Map<Long, String>,
        recordId: Long?,
        matchedText: String?,
    ): GroupSearchItem? {
        val cardListId = listId ?: return null
        val listTitle = listTitles[cardListId] ?: return null
        return GroupSearchItem(
            listId = cardListId,
            listTitle = listTitle,
            cardId = id,
            cardTitle = title,
            recordId = recordId,
            matchedText = matchedText,
        )
    }

    companion object {
        // 한 페이지에 반환하는 결과 수.
        const val PAGE_SIZE: Int = 50

        // 카드명·생성자명 검색 시 한 번에 읽는 카드 수. 카드는 짧은 문자열만 읽는다.
        const val CARD_SCAN_BATCH_SIZE: Int = 200

        // 기록 본문 검색 시 한 번에 읽는 기록 수. 본문은 최대 1MB라 작게 읽는다.
        const val RECORD_SCAN_BATCH_SIZE: Int = 50
    }
}
