package com.sight.core.book

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Component
class Data4LibraryBookClient(
    private val restTemplate: RestTemplate,
    @Value("\${data4library.auth-key}") private val authKey: String,
) : BookInfoClient {
    private val logger = LoggerFactory.getLogger(Data4LibraryBookClient::class.java)

    override fun searchByIsbn(isbn: String): BookInfoItem? {
        // data4library.kr은 Accept 헤더에 */*가 없으면 406을 반환한다 — RestTemplate 기본 Accept는
        // 응답 타입(POJO)을 읽을 수 있는 컨버터(Jackson)만 반영해 application/json 계열로 좁아진다.
        val headers = HttpHeaders().apply { accept = listOf(MediaType.APPLICATION_JSON, MediaType.ALL) }
        val response =
            try {
                restTemplate.exchange(
                    "https://data4library.kr/api/srchDtlList?authKey=$authKey&isbn13=$isbn&format=json",
                    HttpMethod.GET,
                    HttpEntity<Void>(headers),
                    Data4LibraryResponse::class.java,
                )
            } catch (e: RestClientException) {
                logger.error("data4library 도서 조회 실패: isbn={}", isbn, e)
                return null
            }
        val book = response.body?.response?.detail?.firstOrNull()?.book ?: return null
        if (book.bookname.isBlank()) return null
        return BookInfoItem(
            title = book.bookname,
            author = book.authors,
            publisher = book.publisher,
            publishedYear = book.publicationYear.toIntOrNull() ?: 0,
            coverImageUrl = book.bookImageURL,
            description = book.description,
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Data4LibraryResponse(
    val response: Data4LibraryResponseBody,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Data4LibraryResponseBody(
    val detail: List<Data4LibraryDetailWrapper> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Data4LibraryDetailWrapper(
    val book: Data4LibraryBookItem,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Data4LibraryBookItem(
    val bookname: String,
    val authors: String,
    val publisher: String,
    @JsonProperty("publication_year") val publicationYear: String,
    val bookImageURL: String,
    val description: String,
)
