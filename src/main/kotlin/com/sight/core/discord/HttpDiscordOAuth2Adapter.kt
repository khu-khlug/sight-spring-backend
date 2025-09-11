package com.sight.core.discord

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException

@Component
class HttpDiscordOAuth2Adapter(
    @Value("\${discord.oauth2.client-id}")
    private val clientId: String,
    @Value("\${discord.oauth2.client-secret}")
    private val clientSecret: String,
    @Value("\${discord.oauth2.redirect-uri}")
    private val redirectUri: String,
    private val restTemplate: RestTemplate = RestTemplate(),
    private val objectMapper: ObjectMapper = ObjectMapper(),
) : DiscordOAuth2Adapter {
    override suspend fun getAccessToken(code: String): String =
        withContext(Dispatchers.IO) {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_FORM_URLENCODED
                }

            val body =
                LinkedMultiValueMap<String, String>().apply {
                    add("client_id", clientId)
                    add("client_secret", clientSecret)
                    add("grant_type", "authorization_code")
                    add("code", code)
                    add("redirect_uri", redirectUri)
                }

            val request = HttpEntity(body, headers)

            try {
                val response =
                    restTemplate.exchange(
                        "https://discord.com/api/oauth2/token",
                        HttpMethod.POST,
                        request,
                        String::class.java,
                    )

                if (response.statusCode == HttpStatus.OK) {
                    val tokenResponse = objectMapper.readValue(response.body, TokenResponse::class.java)
                    tokenResponse.accessToken
                } else {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "디스코드 액세스 토큰 획득에 실패했습니다")
                }
            } catch (e: Exception) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "디스코드 액세스 토큰 획득에 실패했습니다", e)
            }
        }

    override suspend fun getCurrentUserId(accessToken: String): String =
        withContext(Dispatchers.IO) {
            val headers =
                HttpHeaders().apply {
                    set("Authorization", "Bearer $accessToken")
                }

            val request = HttpEntity<Void>(headers)

            try {
                val response =
                    restTemplate.exchange(
                        "https://discord.com/api/users/@me",
                        HttpMethod.GET,
                        request,
                        String::class.java,
                    )

                if (response.statusCode == HttpStatus.OK) {
                    val userResponse = objectMapper.readValue(response.body, UserResponse::class.java)
                    userResponse.id
                } else {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "디스코드 사용자 정보 조회에 실패했습니다")
                }
            } catch (e: Exception) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "디스코드 사용자 정보 조회에 실패했습니다", e)
            }
        }

    private data class TokenResponse(
        @field:JsonProperty("access_token")
        val accessToken: String,
    )

    private data class UserResponse(
        val id: String,
    )
}
