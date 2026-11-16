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

class GroupSearchServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupSearchRepository = mock<GroupSearchRepository>()
    private val groupSearchService =
        GroupSearchService(
            groupRepository = groupRepository,
            groupSearchRepository = groupSearchRepository,
            groupRecordTextExtractor = GroupRecordTextExtractor(ObjectMapper()),
        )

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

    @Test
    fun `카드명 검색은 기록 아이디와 일치 텍스트 없이 카드 정보를 반환한다`() {
        given(groupRepository.existsById(GROUP_ID)).willReturn(true)
        given(groupSearchRepository.countCardsByTitle(eq(GROUP_ID), any())).willReturn(1)
        given(groupSearchRepository.findCardsByTitleOrderByLatest(eq(GROUP_ID), any(), eq(0L), eq(50)))
            .willReturn(listOf(card(1, "스프링 스터디")))

        val result = groupSearchService.listGroupSearchResults(GROUP_ID, GroupSearchTarget.TITLE, "스프링", 1)

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
    fun `생성자명 검색은 일치 텍스트로 생성자명을 반환한다`() {
        given(groupRepository.existsById(GROUP_ID)).willReturn(true)
        given(groupSearchRepository.countCardsByAuthorName(eq(GROUP_ID), any())).willReturn(1)
        given(groupSearchRepository.findCardsByAuthorNameOrderByLatest(eq(GROUP_ID), any(), eq(0L), eq(50)))
            .willReturn(listOf(card(1, authorName = "홍길동")))

        val result = groupSearchService.listGroupSearchResults(GROUP_ID, GroupSearchTarget.AUTHOR, "길동", 1)

        assertEquals("홍길동", result.results.single().matchedText)
        assertNull(result.results.single().recordId)
    }

    @Test
    fun `결과가 없으면 전체 페이지 수는 0이다`() {
        given(groupRepository.existsById(GROUP_ID)).willReturn(true)
        given(groupSearchRepository.countCardsByTitle(eq(GROUP_ID), any())).willReturn(0)
        given(groupSearchRepository.findCardsByTitleOrderByLatest(eq(GROUP_ID), any(), eq(0L), eq(50))).willReturn(emptyList())

        val result = groupSearchService.listGroupSearchResults(GROUP_ID, GroupSearchTarget.TITLE, "없음", 1)

        assertEquals(0L, result.totalPages)
        assertEquals(0, result.results.size)
    }

    @Test
    fun `결과가 51개면 전체 페이지 수는 2이고 2페이지는 50개 뒤부터 조회한다`() {
        given(groupRepository.existsById(GROUP_ID)).willReturn(true)
        given(groupSearchRepository.countCardsByTitle(eq(GROUP_ID), any())).willReturn(51)
        given(groupSearchRepository.findCardsByTitleOrderByLatest(eq(GROUP_ID), any(), eq(50L), eq(50)))
            .willReturn(listOf(card(51)))

        val result = groupSearchService.listGroupSearchResults(GROUP_ID, GroupSearchTarget.TITLE, "카드", 2)

        assertEquals(2L, result.totalPages)
        assertEquals(1, result.results.size)
    }

    @Test
    fun `기록 검색은 추출한 표시 텍스트가 일치한 기록을 최근 변경순으로 반환한다`() {
        given(groupRepository.existsById(GROUP_ID)).willReturn(true)
        given(groupSearchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, 0L, GroupSearchService.SCAN_BATCH_SIZE))
            .willReturn(
                listOf(
                    legacyRecord(30, 2, "<p>스프링 <b>백엔</b>드</p>"),
                    legacyRecord(20, 1, "<p>스프링만 있음</p>"),
                    legacyRecord(10, 1, "<p>백엔드와 스프링</p>"),
                ),
            )
        given(groupSearchRepository.findCardViewsByIds(eq(GROUP_ID), any()))
            .willReturn(listOf(card(1), card(2)))

        val result = groupSearchService.listGroupSearchResults(GROUP_ID, GroupSearchTarget.RECORD, "백엔드 스프링", 1)

        assertEquals(1L, result.totalPages)
        assertEquals(listOf(30L, 10L), result.results.map { it.recordId })
        assertEquals(listOf(2L, 1L), result.results.map { it.cardId })
        assertEquals("스프링 백엔드", result.results.first().matchedText)
    }

    @Test
    fun `기록 검색은 배치로 나눠 읽고 일치 건수로 페이지를 계산한다`() {
        val batchSize = GroupSearchService.SCAN_BATCH_SIZE
        // 배치 크기만큼 꽉 찬 배치 둘과 1건짜리 마지막 배치. 모두 일치하는 기록이다.
        val full = { start: Int -> (start until start + batchSize).map { legacyRecord(it.toLong(), 1, "<p>스프링 $it</p>") } }
        val last = listOf(legacyRecord(1000, 1, "<p>스프링 마지막</p>"))
        given(groupRepository.existsById(GROUP_ID)).willReturn(true)
        given(groupSearchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, 0L, batchSize)).willReturn(full(1))
        given(groupSearchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, batchSize.toLong(), batchSize))
            .willReturn(full(batchSize + 1))
        given(groupSearchRepository.findSearchableRecordsOrderByLatest(GROUP_ID, batchSize * 2L, batchSize)).willReturn(last)
        given(groupSearchRepository.findCardViewsByIds(eq(GROUP_ID), any())).willReturn(listOf(card(1)))

        val result = groupSearchService.listGroupSearchResults(GROUP_ID, GroupSearchTarget.RECORD, "스프링", 3)

        // 일치 (배치 크기 × 2 + 1)건 = 101건 → 3페이지가 있고, 3페이지에는 마지막 1건만 있다.
        assertEquals(3L, result.totalPages)
        assertEquals(listOf(1000L), result.results.map { it.recordId })
    }

    @Test
    fun `전체 페이지를 넘는 요청은 빈 배열을 반환한다`() {
        given(groupRepository.existsById(GROUP_ID)).willReturn(true)
        given(groupSearchRepository.countCardsByTitle(eq(GROUP_ID), any())).willReturn(1)
        given(groupSearchRepository.findCardsByTitleOrderByLatest(eq(GROUP_ID), any(), eq(4950L), eq(50))).willReturn(emptyList())

        val result = groupSearchService.listGroupSearchResults(GROUP_ID, GroupSearchTarget.TITLE, "카드", 100)

        assertEquals(1L, result.totalPages)
        assertEquals(0, result.results.size)
    }

    @Test
    fun `공백뿐이거나 20자를 넘는 검색어와 1보다 작은 페이지는 거부한다`() {
        val title = GroupSearchTarget.TITLE
        assertThrows<BadRequestException> { groupSearchService.listGroupSearchResults(GROUP_ID, title, "  ", 1) }
        assertThrows<BadRequestException> { groupSearchService.listGroupSearchResults(GROUP_ID, title, "가".repeat(21), 1) }
        assertThrows<BadRequestException> { groupSearchService.listGroupSearchResults(GROUP_ID, title, "카드", 0) }

        verify(groupSearchRepository, never()).countCardsByTitle(any(), any())
    }

    @Test
    fun `그룹이 없으면 찾을 수 없음 오류를 반환한다`() {
        given(groupRepository.existsById(GROUP_ID)).willReturn(false)

        assertThrows<NotFoundException> { groupSearchService.listGroupSearchResults(GROUP_ID, GroupSearchTarget.TITLE, "카드", 1) }
    }

    companion object {
        private const val GROUP_ID = 1L
    }
}
