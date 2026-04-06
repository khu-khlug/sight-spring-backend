package com.sight.core.naver

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class NaverBookClient(
    private val restTemplate: RestTemplate,
    @Value("\${naver.client-id}") private val clientId: String,
    @Value("\${naver.client-secret}") private val clientSecret: String,
) {
    fun searchByIsbn(isbn: String): NaverBookItem? {
        val headers =
            HttpHeaders().apply {
                set("X-Naver-Client-Id", clientId)
                set("X-Naver-Client-Secret", clientSecret)
            }
        val response =
            restTemplate.exchange(
                "https://openapi.naver.com/v1/search/book_adv.json?d_isbn=$isbn",
                HttpMethod.GET,
                HttpEntity<Unit>(headers),
                NaverBookSearchResponse::class.java,
            )
        return response.body?.items?.firstOrNull()
    }
}

data class NaverBookSearchResponse(
    val items: List<NaverBookItem>,
)

data class NaverBookItem(
    val title: String,
    val author: String,
    val publisher: String,
    val pubdate: String,
    val image: String,
    val description: String,
)
