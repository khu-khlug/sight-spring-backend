package com.sight.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupRecordType
import com.sight.repository.GroupRepository
import com.sight.repository.GroupSearchRepository
import com.sight.repository.dto.GroupSearchCardView
import com.sight.repository.dto.GroupSearchRecordView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.text.Normalizer

class GroupSearchServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupSearchRepository = mock<GroupSearchRepository>()
    private val groupSearchService =
        GroupSearchService(
            groupRepository = groupRepository,
            groupSearchRepository = groupSearchRepository,
            groupRecordTextExtractor = GroupRecordTextExtractor(ObjectMapper()),
        )

    private val cardBatch = GroupSearchService.CARD_SCAN_BATCH_SIZE
    private val recordBatch = GroupSearchService.RECORD_SCAN_BATCH_SIZE

    private fun card(
        id: Long,
        title: String = "카드$id",
        authorName: String = "작성자",
    ) = GroupSearchCardView(listId = 10, listTitle = "할 일", cardId = id, cardTitle = title, authorName = authorName)

    private fun legacyRecord(
        id: Long,
        cardId: Long,
        html: String,
    ) = GroupSearchRecordView(recordId = id, cardId = cardId, type = GroupRecordType.LEGACY, content = html)

    private fun givenGroupExists() {
        given(groupRepository.existsById(GROUP_ID)).willReturn(true)
    }

    private fun givenCards(vararg cards: GroupSearchCardView) {
        given(groupSearchRepository.findCardsOrderByLatest(GROUP_ID, 0L, cardBatch)).willReturn(cards.toList())
    }

    private fun search(
        target: GroupSearchTarget,
        keyword: String,
        page: Int = 1,
    ) = groupSearchService.listGroupSearchResults(GROUP_ID, target, keyword, page)

    @Test
    fun `카드명 검색은 기록 아이디와 일치 텍스트 없이 카드 정보를 반환한다`() {
        givenGroupExists()
        givenCards(card(1, "스프링 스터디"), card(2, "프런트"))

        val result = search(GroupSearchTarget.TITLE, "스프링")

        assertEquals(1, result.page)
        assertEquals(1L, result.totalPages)
        val item = result.results.single()
        assertEquals(10L, item.listId)
        assertEquals("할 일", item.listTitle)
        assertEquals(1L, item.cardId)
        assertEquals("스프링 스터디", item.cardTitle)
        assertNull(item.recordId)
        assertNull(item.matchedText)
    }

    @Test
    fun `생성자명 검색은 일치 텍스트로 생성자명을 반환하고 카드명은 검색하지 않는다`() {
        givenGroupExists()
        givenCards(card(1, "카드", authorName = "홍길동"), card(2, "길동 카드", authorName = "김민수"))

        val result = search(GroupSearchTarget.AUTHOR, "길동")

        assertEquals(listOf(1L), result.results.map { it.cardId })
        assertEquals("홍길동", result.results.single().matchedText)
        assertNull(result.results.single().recordId)
    }

    @Test
    fun `모든 검색어가 들어 있는 카드만 최근 변경순으로 반환한다`() {
        givenGroupExists()
        givenCards(card(3, "백엔드 스프링"), card(2, "스프링만"), card(1, "스프링 그리고 백엔드"))

        val result = search(GroupSearchTarget.TITLE, "백엔드 스프링")

        assertEquals(listOf(3L, 1L), result.results.map { it.cardId })
    }

    @Test
    fun `카드명은 악센트와 전각 문자를 구분하고 대소문자는 구분하지 않는다`() {
        givenGroupExists()
        givenCards(card(1, "café"), card(2, "cafe"), card(3, "ＣＡＦＥ"), card(4, "CAFE"))

        val result = search(GroupSearchTarget.TITLE, "cafe")

        assertEquals(listOf(2L, 4L), result.results.map { it.cardId })
    }

    @Test
    fun `풀어쓴 형태로 저장된 카드명도 합친 형태의 검색어로 찾는다`() {
        givenGroupExists()
        givenCards(card(1, Normalizer.normalize("한글 스터디", Normalizer.Form.NFD)))

        val result = search(GroupSearchTarget.TITLE, "한글")

        assertEquals(listOf(1L), result.results.map { it.cardId })
    }

    @Test
    fun `결과가 없으면 전체 페이지 수는 0이다`() {
        givenGroupExists()
        givenCards(card(1, "관계없음"))

        val result = search(GroupSearchTarget.TITLE, "없음없음")

        assertEquals(0L, result.totalPages)
        assertEquals(0, result.results.size)
    }

    @Test
    fun `결과가 51개면 전체 페이지 수는 2이고 2페이지에는 1개가 있다`() {
        givenGroupExists()
        val cards = (1L..51L).map { card(it, "카드 $it") }.toTypedArray()
        givenCards(*cards)

        val first = search(GroupSearchTarget.TITLE, "카드", page = 1)
        val second = search(GroupSearchTarget.TITLE, "카드", page = 2)

        assertEquals(2L, first.totalPages)
        assertEquals(50, first.results.size)
        assertEquals(1, second.results.size)
        assertEquals(51L, second.results.single().cardId)
    }

    @Test
    fun `전체 페이지를 넘는 요청은 빈 배열을 반환한다`() {
        givenGroupExists()
        givenCards(card(1, "카드 하나"))

        val result = search(GroupSearchTarget.TITLE, "카드", page = 100)

        assertEquals(1L, result.totalPages)
        assertEquals(0, result.results.size)
    }

    @Test
    fun `카드는 배치로 나눠 읽고 일치 건수로 페이지를 계산한다`() {
        givenGroupExists()
        val first = (1L..cardBatch.toLong()).map { card(it, "스프링 $it") }
        given(groupSearchRepository.findCardsOrderByLatest(GROUP_ID, 0L, cardBatch)).willReturn(first)
        given(groupSearchRepository.findCardsOrderByLatest(GROUP_ID, cardBatch.toLong(), cardBatch))
            .willReturn(listOf(card(1000, "스프링 마지막")))

        val result = search(GroupSearchTarget.TITLE, "스프링", page = 5)

        // 일치 201건 → 5페이지가 있고, 5페이지에는 마지막 1건만 있다.
        assertEquals(5L, result.totalPages)
        assertEquals(listOf(1000L), result.results.map { it.cardId })
    }

    @Test
    fun `기록 검색은 추출한 표시 텍스트가 일치한 기록을 최근 변경순으로 반환한다`() {
        givenGroupExists()
        given(groupSearchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, 0L, recordBatch))
            .willReturn(
                listOf(
                    legacyRecord(30, 2, "<p>스프링 <b>백엔</b>드</p>"),
                    legacyRecord(20, 1, "<p>스프링만 있음</p>"),
                    legacyRecord(10, 1, "<p>백엔드와 스프링</p>"),
                ),
            )
        given(groupSearchRepository.findCardViewsByIds(eq(GROUP_ID), any())).willReturn(listOf(card(1), card(2)))

        val result = search(GroupSearchTarget.RECORD, "백엔드 스프링")

        assertEquals(1L, result.totalPages)
        assertEquals(listOf(30L, 10L), result.results.map { it.recordId })
        assertEquals(listOf(2L, 1L), result.results.map { it.cardId })
        assertEquals("스프링 백엔드", result.results.first().matchedText)
    }

    @Test
    fun `기록 검색도 악센트를 구분하고 풀어쓴 한글을 정규화해 찾는다`() {
        givenGroupExists()
        given(groupSearchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, 0L, recordBatch))
            .willReturn(
                listOf(
                    legacyRecord(3, 1, "<p>café</p>"),
                    legacyRecord(2, 1, "<p>cafe</p>"),
                    legacyRecord(1, 1, "<p>${Normalizer.normalize("한글", Normalizer.Form.NFD)}</p>"),
                ),
            )
        given(groupSearchRepository.findCardViewsByIds(eq(GROUP_ID), any())).willReturn(listOf(card(1)))

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
        given(groupSearchRepository.findCardViewsByIds(eq(GROUP_ID), any())).willReturn(listOf(card(1)))

        val result = search(GroupSearchTarget.RECORD, "스프링", page = 3)

        // 일치 (배치 크기 × 2 + 1)건 = 101건 → 3페이지가 있고, 3페이지에는 마지막 1건만 있다.
        assertEquals(3L, result.totalPages)
        assertEquals(listOf(1000L), result.results.map { it.recordId })
    }

    @Test
    fun `공백뿐이거나 20자를 넘는 검색어는 거부한다`() {
        assertThrows<BadRequestException> { search(GroupSearchTarget.TITLE, "  ") }
        assertThrows<BadRequestException> { search(GroupSearchTarget.TITLE, "가".repeat(21)) }

        verify(groupSearchRepository, never()).findCardsOrderByLatest(any(), any(), any())
    }

    @Test
    fun `이모지는 한 글자로 세어 20개까지 허용하고 21개는 거부한다`() {
        givenGroupExists()
        givenCards()

        search(GroupSearchTarget.TITLE, "😀".repeat(20))

        assertThrows<BadRequestException> { search(GroupSearchTarget.TITLE, "😀".repeat(21)) }
    }

    @Test
    fun `그룹이 없으면 찾을 수 없음 오류를 반환한다`() {
        given(groupRepository.existsById(GROUP_ID)).willReturn(false)

        assertThrows<NotFoundException> { search(GroupSearchTarget.TITLE, "카드") }
    }

    companion object {
        private const val GROUP_ID = 1L
    }
}
