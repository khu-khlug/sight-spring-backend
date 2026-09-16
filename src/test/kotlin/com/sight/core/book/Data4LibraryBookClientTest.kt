package com.sight.core.book

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Data4LibraryBookClientTest {
    private val restTemplate = mock<RestTemplate>()
    private val client = Data4LibraryBookClient(restTemplate, "test-auth-key")

    private fun stubResponse(book: Data4LibraryBookItem?) {
        val detail = if (book != null) listOf(Data4LibraryDetailWrapper(book)) else emptyList()
        val response = Data4LibraryResponse(Data4LibraryResponseBody(detail))
        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                any(),
                eq(Data4LibraryResponse::class.java),
            ),
        ).thenReturn(ResponseEntity.ok(response))
    }

    @Test
    fun `searchByIsbn은 정상 응답을 BookInfoItem으로 매핑한다`() {
        // given
        stubResponse(
            Data4LibraryBookItem(
                bookname = "테스트 도서",
                authors = "저자",
                publisher = "출판사",
                publicationYear = "2024",
                bookImageURL = "https://example.com/cover.jpg",
                description = "설명",
            ),
        )

        // when
        val result = client.searchByIsbn("9780000000001")

        // then
        assertEquals("테스트 도서", result?.title)
        assertEquals("저자", result?.author)
        assertEquals("출판사", result?.publisher)
        assertEquals(2024, result?.publishedYear)
        assertEquals("https://example.com/cover.jpg", result?.coverImageUrl)
        assertEquals("설명", result?.description)
    }

    @Test
    fun `searchByIsbn은 bookname이 빈 문자열이면 null을 반환한다`() {
        // given
        stubResponse(
            Data4LibraryBookItem(
                bookname = "",
                authors = "저자",
                publisher = "출판사",
                publicationYear = "2024",
                bookImageURL = "https://example.com/cover.jpg",
                description = "설명",
            ),
        )

        // when
        val result = client.searchByIsbn("9780000000001")

        // then
        assertNull(result)
    }

    @Test
    fun `searchByIsbn은 publication_year가 숫자가 아니면 publishedYear를 0으로 반환한다`() {
        // given
        stubResponse(
            Data4LibraryBookItem(
                bookname = "테스트 도서",
                authors = "저자",
                publisher = "출판사",
                publicationYear = "",
                bookImageURL = "https://example.com/cover.jpg",
                description = "설명",
            ),
        )

        // when
        val result = client.searchByIsbn("9780000000001")

        // then
        assertEquals(0, result?.publishedYear)
    }

    @Test
    fun `searchByIsbn은 검색 결과가 없으면 null을 반환한다`() {
        // given
        stubResponse(null)

        // when
        val result = client.searchByIsbn("9780000000001")

        // then
        assertNull(result)
    }

    @Test
    fun `searchByIsbn은 네트워크 오류가 발생하면 null을 반환한다`() {
        // given
        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                any(),
                eq(Data4LibraryResponse::class.java),
            ),
        ).thenThrow(ResourceAccessException("connection timed out"))

        // when
        val result = client.searchByIsbn("9780000000001")

        // then
        assertNull(result)
    }
}
