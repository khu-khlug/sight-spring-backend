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

        // 페이지는 0부터 시작한다.
        val offset = page.toLong() * PAGE_SIZE
        // 대상과 관계없이 같은 비교 규칙(GroupSearchKeywords)으로 일치 여부를 판정하고, 결과는 한 곳에서 만든다.
        val scan =
            when (target) {
                GroupSearchTarget.TITLE -> searchCards(groupId, keywords, offset, byAuthorName = false)
                GroupSearchTarget.AUTHOR -> searchCards(groupId, keywords, offset, byAuthorName = true)
                GroupSearchTarget.RECORD -> searchRecords(groupId, keywords, offset)
            }
        return GroupSearchResult(page = page, totalPages = totalPages(scan.matchedCount), results = scan.items)
    }

    // 카드명 또는 카드 생성자명 검색. 카드·리스트·회원을 각 Repository에서 객체로 읽어 이 서비스에서 조합한다.
    // 카드 엔티티는 영속성 컨텍스트에 트랜잭션이 끝날 때까지 남지만 짧은 필드만 가지므로 크기가 작다.
    private fun searchCards(
        groupId: Long,
        keywords: GroupSearchKeywords,
        offset: Long,
        byAuthorName: Boolean,
    ): ScanResult {
        val lookup = Lookup(groupId)
        return scan(
            keywords = keywords,
            offset = offset,
            batchSize = CARD_SCAN_BATCH_SIZE,
            fetch = { start -> fetchCards(groupId, start) },
            prepare = { cards ->
                lookup.loadLists(cards.mapNotNull { it.listId })
                if (byAuthorName) lookup.loadAuthorNames(cards)
            },
            accepts = { card -> lookup.hasList(card) },
            textOf = { card -> if (byAuthorName) lookup.authorNameOf(card) else card.title },
            toItem = { card, _ ->
                card.toItem(lookup, recordId = null, matchedText = if (byAuthorName) lookup.authorNameOf(card) else null)
            },
        )
    }

    // 기록 본문 검색. 저장 형식(HTML·JSON)에서 표시 텍스트를 추출해야 일치 여부를 알 수 있다.
    // 기록은 본문이 커서 엔티티로 받지 않고 View로 읽는다(GroupSearchRepository 참고).
    private fun searchRecords(
        groupId: Long,
        keywords: GroupSearchKeywords,
        offset: Long,
    ): ScanResult {
        val lookup = Lookup(groupId)
        return scan(
            keywords = keywords,
            offset = offset,
            batchSize = RECORD_SCAN_BATCH_SIZE,
            fetch = { start -> groupSearchRepository.findSearchableRecordsOrderByLatest(groupId, start, RECORD_SCAN_BATCH_SIZE) },
            prepare = { records ->
                lookup.loadCards(records.map { it.cardId })
                lookup.loadLists(records.mapNotNull { lookup.cardOf(it.cardId)?.listId })
            },
            accepts = { record -> lookup.cardOf(record.cardId)?.let { lookup.hasList(it) } == true },
            textOf = { record -> groupRecordTextExtractor.extract(record.type, record.content) },
            // 본문(최대 1MB)을 붙들지 않도록 일치한 시점에 미리보기만 만들어 결과에 담는다.
            toItem = { record, match ->
                val card = checkNotNull(lookup.cardOf(record.cardId)) { "수락한 기록의 카드를 찾을 수 없습니다." }
                card.toItem(lookup, recordId = record.recordId, matchedText = GroupSearchPreview.create(match))
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

    // 최근 변경순으로 배치 단위로 읽으며 일치 건수를 센다.
    // - `prepare`: 읽은 배치에서 필요한 카드·리스트·회원을 한꺼번에 읽어 둔다.
    // - `accepts`: 결과에 담을 수 없는 항목(예: 리스트를 찾을 수 없는 카드)은 일치 판정과 건수 계산 전에 걸러낸다.
    //   나중에 결과를 만들 때 빠지면 한 페이지가 50개보다 적게 채워지고 전체 페이지 수와 어긋나기 때문이다.
    // - `toItem`: 요청한 페이지에 해당하는 항목만 결과로 바꿔 보관하고, 읽은 배치와 일치 정보(정규화한 본문 전체)는 바로 버린다.
    private fun <T> scan(
        keywords: GroupSearchKeywords,
        offset: Long,
        batchSize: Int,
        fetch: (Long) -> List<T>,
        prepare: (List<T>) -> Unit,
        accepts: (T) -> Boolean,
        textOf: (T) -> String,
        toItem: (T, GroupSearchKeywords.Match) -> GroupSearchItem,
    ): ScanResult {
        val items = mutableListOf<GroupSearchItem>()
        var matchedCount = 0L
        var scanned = 0L
        while (true) {
            val batch = fetch(scanned)
            prepare(batch)
            batch.forEach { entry ->
                if (!accepts(entry)) return@forEach
                val match = keywords.find(textOf(entry)) ?: return@forEach
                if (matchedCount >= offset && items.size < PAGE_SIZE) items += toItem(entry, match)
                matchedCount++
            }
            scanned += batch.size
            if (batch.size < batchSize) break
        }
        return ScanResult(matchedCount, items)
    }

    private class ScanResult(
        val matchedCount: Long,
        val items: List<GroupSearchItem>,
    )

    // 한 번의 검색 동안 읽은 카드·리스트·회원을 모아 둔다. 같은 아이디를 다시 조회하지 않고, 찾지 못한 아이디도 다시 묻지 않는다.
    private inner class Lookup(
        private val groupId: Long,
    ) {
        private val cards = mutableMapOf<Long, GroupCard>()
        private val listTitles = mutableMapOf<Long, String>()
        private val authorNames = mutableMapOf<Long, String>()
        private val requestedCardIds = mutableSetOf<Long>()
        private val requestedListIds = mutableSetOf<Long>()
        private val requestedMemberIds = mutableSetOf<Long>()

        fun loadCards(cardIds: Collection<Long>) {
            val missing = cardIds.filter { requestedCardIds.add(it) }.toSet()
            if (missing.isEmpty()) return
            groupCardRepository
                .findAllById(missing)
                .filter { it.groupId == groupId && !it.isDeleted }
                .forEach { cards[it.id] = it }
        }

        fun loadLists(listIds: Collection<Long>) {
            val missing = listIds.filter { requestedListIds.add(it) }.toSet()
            if (missing.isEmpty()) return
            groupKanbanListRepository.findAllById(missing).forEach { listTitles[it.id] = it.title }
        }

        // 탈퇴해도 회원 행과 실명은 남는다.
        fun loadAuthorNames(cards: List<GroupCard>) {
            val missing = cards.map { it.authorMemberId }.filter { requestedMemberIds.add(it) }.toSet()
            if (missing.isEmpty()) return
            memberRepository.findAllById(missing).forEach { authorNames[it.id] = it.realname }
        }

        fun cardOf(cardId: Long): GroupCard? = cards[cardId]

        // 삭제되지 않은 카드는 항상 리스트에 속한다. 리스트를 찾을 수 없으면 검색 결과에 담을 수 없는 카드다.
        fun hasList(card: GroupCard): Boolean = card.listId?.let { it in listTitles } == true

        fun listTitleOf(card: GroupCard): String = checkNotNull(card.listId?.let { listTitles[it] }) { "리스트를 찾을 수 없는 카드입니다." }

        fun authorNameOf(card: GroupCard): String = authorNames[card.authorMemberId].orEmpty()
    }

    private fun totalPages(totalCount: Long): Long = (totalCount + PAGE_SIZE - 1) / PAGE_SIZE

    private fun GroupCard.toItem(
        lookup: Lookup,
        recordId: Long?,
        matchedText: String?,
    ): GroupSearchItem =
        GroupSearchItem(
            listId = checkNotNull(listId) { "리스트를 찾을 수 없는 카드입니다." },
            listTitle = lookup.listTitleOf(this),
            cardId = id,
            cardTitle = title,
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
