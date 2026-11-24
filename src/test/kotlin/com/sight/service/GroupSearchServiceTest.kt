package com.sight.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupCard
import com.sight.domain.group.GroupKanbanList
import com.sight.domain.group.GroupRecordType
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.repository.GroupCardRepository
import com.sight.repository.GroupKanbanListRepository
import com.sight.repository.GroupRepository
import com.sight.repository.GroupSearchRepository
import com.sight.repository.MemberRepository
import com.sight.repository.dto.GroupSearchRecordView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.text.Normalizer
import java.time.Instant

class GroupSearchServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupCardRepository = mock<GroupCardRepository>()
    private val groupKanbanListRepository = mock<GroupKanbanListRepository>()
    private val memberRepository = mock<MemberRepository>()
    private val groupSearchRepository = mock<GroupSearchRepository>()
    private val groupSearchService =
        GroupSearchService(
            groupRepository = groupRepository,
            groupCardRepository = groupCardRepository,
            groupKanbanListRepository = groupKanbanListRepository,
            memberRepository = memberRepository,
            groupSearchRepository = groupSearchRepository,
            groupRecordTextExtractor = GroupRecordTextExtractor(ObjectMapper()),
        )

    private val cardBatch = GroupSearchService.CARD_SCAN_BATCH_SIZE
    private val recordBatch = GroupSearchService.RECORD_SCAN_BATCH_SIZE

    private fun card(
        id: Long,
        title: String = "카드$id",
        authorId: Long = AUTHOR_ID,
        groupId: Long = GROUP_ID,
        listId: Long = LIST_ID,
    ) = GroupCard(
        id = id,
        groupId = groupId,
        listId = listId,
        authorMemberId = authorId,
        title = title,
        position = id,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
    )

    private fun member(
        id: Long,
        realname: String,
    ) = Member(
        id = id,
        name = "user$id",
        realname = realname,
        admission = "19",
        college = "공과대학",
        grade = 3L,
        studentStatus = StudentStatus.UNDERGRADUATE,
        status = UserStatus.ACTIVE,
    )

    private fun legacyRecord(
        id: Long,
        cardId: Long,
        html: String,
    ) = GroupSearchRecordView(recordId = id, cardId = cardId, type = GroupRecordType.LEGACY, content = html)

    // 그룹과 리스트, 기본 회원을 준비한다.
    private fun givenGroupExists(vararg members: Member) {
        given(groupRepository.existsById(GROUP_ID)).willReturn(true)
        given(groupKanbanListRepository.findAllById(any<Iterable<Long>>()))
            .willReturn(listOf(GroupKanbanList(LIST_ID, GROUP_ID, "할 일", 1, Instant.parse("2025-01-01T00:00:00Z"))))
        val all = members.toList().ifEmpty { listOf(member(AUTHOR_ID, "작성자")) }
        given(memberRepository.findAllById(any<Iterable<Long>>())).willAnswer { invocation ->
            val ids = invocation.getArgument<Iterable<Long>>(0).toSet()
            all.filter { it.id in ids }
        }
    }

    // 카드 조회는 페이지 번호(배치 순번)별로 준비한다.
    private fun givenCardPage(
        index: Int,
        cards: List<GroupCard>,
    ) {
        given(
            groupCardRepository.findByGroupIdAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(
                eq(GROUP_ID),
                argThat<Pageable> { pageNumber == index && pageSize == cardBatch },
            ),
        ).willReturn(cards)
    }

    private fun search(
        target: GroupSearchTarget,
        keyword: String,
        page: Int = 0,
    ) = groupSearchService.listGroupSearchResults(GROUP_ID, target, keyword, page)

    @Test
    fun `카드명 검색은 기록 아이디와 일치 텍스트 없이 카드와 리스트 정보를 반환한다`() {
        givenGroupExists()
        givenCardPage(0, listOf(card(1, "스프링 스터디"), card(2, "프런트")))

        val result = search(GroupSearchTarget.TITLE, "스프링")

        assertEquals(0, result.page)
        assertEquals(1L, result.totalPages)
        val item = result.results.single()
        assertEquals(LIST_ID, item.listId)
        assertEquals("할 일", item.listTitle)
        assertEquals(1L, item.cardId)
        assertEquals("스프링 스터디", item.cardTitle)
        assertNull(item.recordId)
        assertNull(item.matchedText)
    }

    @Test
    fun `카드명 검색은 회원을 조회하지 않는다`() {
        givenGroupExists()
        givenCardPage(0, listOf(card(1, "스프링 스터디")))

        search(GroupSearchTarget.TITLE, "스프링")

        verifyNoInteractions(memberRepository)
    }

    @Test
    fun `생성자명 검색은 일치 텍스트로 생성자명을 반환하고 카드명은 검색하지 않는다`() {
        givenGroupExists(member(1, "홍길동"), member(2, "김민수"))
        givenCardPage(0, listOf(card(1, "카드", authorId = 1), card(2, "길동 카드", authorId = 2)))

        val result = search(GroupSearchTarget.AUTHOR, "길동")

        assertEquals(listOf(1L), result.results.map { it.cardId })
        assertEquals("홍길동", result.results.single().matchedText)
        assertNull(result.results.single().recordId)
    }

    @Test
    fun `생성자 회원은 이미 읽은 회원을 다시 조회하지 않고 배치마다 새 회원만 조회한다`() {
        givenGroupExists(member(1, "홍길동"), member(2, "김민수"), member(3, "이영희"))
        val first = (1L..cardBatch.toLong()).map { card(it, "카드 $it", authorId = if (it <= 100) 1 else 2) }
        givenCardPage(0, first)
        givenCardPage(1, listOf(card(1000, "마지막", authorId = 3)))

        val result = search(GroupSearchTarget.AUTHOR, "영희")

        assertEquals(listOf(1000L), result.results.map { it.cardId })
        verify(memberRepository).findAllById(eq(setOf(1L, 2L)))
        verify(memberRepository).findAllById(eq(setOf(3L)))
        verify(memberRepository, times(2)).findAllById(any<Iterable<Long>>())
    }

    @Test
    fun `모든 검색어가 들어 있는 카드만 최근 변경순으로 반환한다`() {
        givenGroupExists()
        givenCardPage(0, listOf(card(3, "백엔드 스프링"), card(2, "스프링만"), card(1, "스프링 그리고 백엔드")))

        val result = search(GroupSearchTarget.TITLE, "백엔드 스프링")

        assertEquals(listOf(3L, 1L), result.results.map { it.cardId })
    }

    @Test
    fun `카드명은 악센트와 전각 문자를 구분하고 대소문자는 구분하지 않는다`() {
        givenGroupExists()
        givenCardPage(0, listOf(card(1, "café"), card(2, "cafe"), card(3, "ＣＡＦＥ"), card(4, "CAFE")))

        val result = search(GroupSearchTarget.TITLE, "cafe")

        assertEquals(listOf(2L, 4L), result.results.map { it.cardId })
    }

    @Test
    fun `풀어쓴 형태로 저장된 카드명도 합친 형태의 검색어로 찾는다`() {
        givenGroupExists()
        givenCardPage(0, listOf(card(1, Normalizer.normalize("한글 스터디", Normalizer.Form.NFD))))

        val result = search(GroupSearchTarget.TITLE, "한글")

        assertEquals(listOf(1L), result.results.map { it.cardId })
    }

    @Test
    fun `결과가 없으면 전체 페이지 수는 0이다`() {
        givenGroupExists()
        givenCardPage(0, listOf(card(1, "관계없음")))

        val result = search(GroupSearchTarget.TITLE, "없음없음")

        assertEquals(0L, result.totalPages)
        assertEquals(0, result.results.size)
    }

    @Test
    fun `결과가 51개면 전체 페이지 수는 2이고 두 번째 페이지(page=1)에는 1개가 있다`() {
        givenGroupExists()
        givenCardPage(0, (1L..51L).map { card(it, "카드 $it") })

        val first = search(GroupSearchTarget.TITLE, "카드", page = 0)
        val second = search(GroupSearchTarget.TITLE, "카드", page = 1)

        assertEquals(2L, first.totalPages)
        assertEquals(50, first.results.size)
        assertEquals(1, second.results.size)
        assertEquals(51L, second.results.single().cardId)
    }

    @Test
    fun `리스트를 찾을 수 없는 카드는 일치 건수와 결과에서 처음부터 제외해 한 페이지를 채운다`() {
        givenGroupExists()
        // 최신 카드 하나는 존재하지 않는 리스트에 속해 있다. 나중에 빼면 첫 페이지가 49개가 된다.
        val dangling = card(9999, "카드 리스트없음", listId = 999)
        givenCardPage(0, listOf(dangling) + (1L..51L).map { card(it, "카드 $it") })

        val first = search(GroupSearchTarget.TITLE, "카드", page = 0)
        val second = search(GroupSearchTarget.TITLE, "카드", page = 1)

        assertEquals(2L, first.totalPages)
        assertEquals(50, first.results.size)
        assertEquals(1, second.results.size)
        assertEquals(false, (first.results + second.results).any { it.cardId == 9999L })
    }

    @Test
    fun `삭제되어 리스트가 없는 카드는 결과와 일치 건수에서 제외한다`() {
        givenGroupExists()
        val deleted = card(2, "카드 삭제됨").also { it.delete(Instant.parse("2025-01-02T00:00:00Z")) }
        givenCardPage(0, listOf(card(1, "카드 하나"), deleted))

        val result = search(GroupSearchTarget.TITLE, "카드")

        assertEquals(listOf(1L), result.results.map { it.cardId })
        assertEquals(1L, result.totalPages)
    }

    @Test
    fun `생성자명 검색도 리스트를 찾을 수 없는 카드를 제외한다`() {
        givenGroupExists(member(1, "홍길동"))
        givenCardPage(0, listOf(card(1, "카드", authorId = 1, listId = 999), card(2, "카드", authorId = 1)))

        val result = search(GroupSearchTarget.AUTHOR, "길동")

        assertEquals(listOf(2L), result.results.map { it.cardId })
    }

    @Test
    fun `기록 검색도 리스트를 찾을 수 없는 카드의 기록은 처음부터 제외해 한 페이지를 채운다`() {
        givenGroupExists()
        val records = listOf(legacyRecord(9999, 9, "<p>스프링</p>")) + (1L..51L).map { legacyRecord(it, 1, "<p>스프링 $it</p>") }
        givenRecordsAndCards(records, card(1), card(9, listId = 999))

        val first = search(GroupSearchTarget.RECORD, "스프링", page = 0)
        val second = search(GroupSearchTarget.RECORD, "스프링", page = 1)

        assertEquals(2L, first.totalPages)
        assertEquals(50, first.results.size)
        assertEquals(1, second.results.size)
        assertEquals(false, (first.results + second.results).any { it.recordId == 9999L })
    }

    @Test
    fun `전체 페이지를 넘는 요청은 빈 배열을 반환한다`() {
        givenGroupExists()
        givenCardPage(0, listOf(card(1, "카드 하나")))

        val result = search(GroupSearchTarget.TITLE, "카드", page = 99)

        assertEquals(1L, result.totalPages)
        assertEquals(0, result.results.size)
    }

    @Test
    fun `카드는 배치로 나눠 읽고 일치 건수로 페이지를 계산한다`() {
        givenGroupExists()
        givenCardPage(0, (1L..cardBatch.toLong()).map { card(it, "스프링 $it") })
        givenCardPage(1, listOf(card(1000, "스프링 마지막")))

        val result = search(GroupSearchTarget.TITLE, "스프링", page = 4)

        // 일치 201건 → 5페이지가 있고, 마지막 페이지(page=4)에는 마지막 1건만 있다.
        assertEquals(5L, result.totalPages)
        assertEquals(listOf(1000L), result.results.map { it.cardId })
    }

    private fun givenRecordsAndCards(
        records: List<GroupSearchRecordView>,
        vararg cards: GroupCard,
    ) {
        given(groupSearchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, 0L, recordBatch)).willReturn(records)
        given(groupCardRepository.findAllById(any<Iterable<Long>>())).willReturn(cards.toList())
    }

    @Test
    fun `기록 검색은 추출한 표시 텍스트가 일치한 기록을 최근 변경순으로 반환한다`() {
        givenGroupExists()
        givenRecordsAndCards(
            listOf(
                legacyRecord(30, 2, "<p>스프링 <b>백엔</b>드</p>"),
                legacyRecord(20, 1, "<p>스프링만 있음</p>"),
                legacyRecord(10, 1, "<p>백엔드와 스프링</p>"),
            ),
            card(1),
            card(2),
        )

        val result = search(GroupSearchTarget.RECORD, "백엔드 스프링")

        assertEquals(1L, result.totalPages)
        assertEquals(listOf(30L, 10L), result.results.map { it.recordId })
        assertEquals(listOf(2L, 1L), result.results.map { it.cardId })
        assertEquals("할 일", result.results.first().listTitle)
        assertEquals("스프링 백엔드", result.results.first().matchedText)
    }

    @Test
    fun `기록 검색은 다른 그룹이나 삭제된 카드의 기록을 결과에서 뺀다`() {
        givenGroupExists()
        givenRecordsAndCards(
            listOf(legacyRecord(3, 1, "<p>스프링</p>"), legacyRecord(2, 2, "<p>스프링</p>"), legacyRecord(1, 3, "<p>스프링</p>")),
            card(1),
            card(2, groupId = OTHER_GROUP_ID),
            card(3).also { it.delete(Instant.parse("2025-01-02T00:00:00Z")) },
        )

        val result = search(GroupSearchTarget.RECORD, "스프링")

        assertEquals(listOf(3L), result.results.map { it.recordId })
    }

    @Test
    fun `기록 검색도 악센트를 구분하고 풀어쓴 한글을 정규화해 찾는다`() {
        givenGroupExists()
        givenRecordsAndCards(
            listOf(
                legacyRecord(3, 1, "<p>café</p>"),
                legacyRecord(2, 1, "<p>cafe</p>"),
                legacyRecord(1, 1, "<p>${Normalizer.normalize("한글", Normalizer.Form.NFD)}</p>"),
            ),
            card(1),
        )

        assertEquals(listOf(2L), search(GroupSearchTarget.RECORD, "cafe").results.map { it.recordId })
        assertEquals(listOf(1L), search(GroupSearchTarget.RECORD, "한글").results.map { it.recordId })
    }

    @Test
    fun `기록 검색은 배치로 나눠 읽고 일치 건수로 페이지를 계산한다`() {
        givenGroupExists()
        // 배치 크기만큼 꽉 찬 배치 둘과 1건짜리 마지막 배치. 모두 일치하는 기록이다.
        val full = { start: Int -> (start until start + recordBatch).map { legacyRecord(it.toLong(), 1, "<p>스프링 $it</p>") } }
        given(groupSearchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, 0L, recordBatch)).willReturn(full(1))
        given(groupSearchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, recordBatch.toLong(), recordBatch))
            .willReturn(full(recordBatch + 1))
        given(groupSearchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, recordBatch * 2L, recordBatch))
            .willReturn(listOf(legacyRecord(1000, 1, "<p>스프링 마지막</p>")))
        given(groupCardRepository.findAllById(any<Iterable<Long>>())).willReturn(listOf(card(1)))

        val result = search(GroupSearchTarget.RECORD, "스프링", page = 2)

        // 일치 (배치 크기 × 2 + 1)건 = 101건 → 3페이지가 있고, 마지막 페이지(page=2)에는 마지막 1건만 있다.
        assertEquals(3L, result.totalPages)
        assertEquals(listOf(1000L), result.results.map { it.recordId })
    }

    @Test
    fun `일치한 기록이 많아도 요청한 페이지의 결과만 미리보기와 함께 반환한다`() {
        givenGroupExists()
        val big = "<p>" + "가".repeat(50_000) + " 스프링 " + "나".repeat(50_000) + "</p>"
        val records = (1L..60L).map { legacyRecord(it, 1, big) }
        given(groupSearchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, 0L, recordBatch)).willReturn(records.take(recordBatch))
        given(groupSearchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, recordBatch.toLong(), recordBatch))
            .willReturn(records.drop(recordBatch))
        given(groupCardRepository.findAllById(any<Iterable<Long>>())).willReturn(listOf(card(1)))

        val result = search(GroupSearchTarget.RECORD, "스프링", page = 0)

        assertEquals(2L, result.totalPages)
        assertEquals(50, result.results.size)
        // 미리보기는 일치 위치 주변만 담고 본문 전체(10만 자)를 담지 않는다.
        assertEquals(true, result.results.all { (it.matchedText ?: "").length < 300 })
    }

    @Test
    fun `공백뿐이거나 20자를 넘는 검색어는 거부한다`() {
        givenGroupExists()

        assertThrows<BadRequestException> { search(GroupSearchTarget.TITLE, "  ") }
        assertThrows<BadRequestException> { search(GroupSearchTarget.TITLE, "가".repeat(21)) }

        verify(groupCardRepository, never()).findByGroupIdAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(any(), any())
    }

    @Test
    fun `이모지는 한 글자로 세어 20개까지 허용하고 21개는 거부한다`() {
        givenGroupExists()

        search(GroupSearchTarget.TITLE, "😀".repeat(20))

        assertThrows<BadRequestException> { search(GroupSearchTarget.TITLE, "😀".repeat(21)) }
    }

    @Test
    fun `그룹이 없으면 찾을 수 없음 오류를 반환한다`() {
        given(groupRepository.existsById(GROUP_ID)).willReturn(false)

        assertThrows<NotFoundException> { search(GroupSearchTarget.TITLE, "카드") }
    }

    @Test
    fun `없는 그룹에 잘못된 검색어를 보내도 검색어 오류가 아니라 찾을 수 없음 오류를 반환한다`() {
        given(groupRepository.existsById(GROUP_ID)).willReturn(false)

        assertThrows<NotFoundException> { search(GroupSearchTarget.TITLE, "  ") }
        assertThrows<NotFoundException> { search(GroupSearchTarget.TITLE, "가".repeat(21)) }
    }

    @Test
    fun `한 번의 검색은 서버 기본 설정과 관계없이 스냅샷으로 일관되게 읽는다`() {
        val transactional =
            GroupSearchService::class.java
                .getMethod("listGroupSearchResults", Long::class.java, GroupSearchTarget::class.java, String::class.java, Int::class.java)
                .getAnnotation(Transactional::class.java)

        assertEquals(true, transactional.readOnly)
        assertEquals(Isolation.REPEATABLE_READ, transactional.isolation)
    }

    companion object {
        private const val GROUP_ID = 1L
        private const val OTHER_GROUP_ID = 2L
        private const val LIST_ID = 10L
        private const val AUTHOR_ID = 1L
    }
}
