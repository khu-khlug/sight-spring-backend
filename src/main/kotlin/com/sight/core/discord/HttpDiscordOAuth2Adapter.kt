package com.sight.core.discord

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.config.DiscordConfig
import com.sight.core.exception.BadRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

@Component
class HttpDiscordOAuth2Adapter(
    @Value("\${discord.oauth2.client-id}")
    private val clientId: String,
    @Value("\${discord.oauth2.client-secret}")
    private val clientSecret: String,
    @Value("\${discord.oauth2.redirect-uri}")
    private val redirectUri: String,
    @Qualifier("discordRestTemplate")
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper = ObjectMapper(),
    private val discordConfig: DiscordConfig,
) : DiscordOAuth2Adapter {
    private val logger = LoggerFactory.getLogger(HttpDiscordOAuth2Adapter::class.java)

    override suspend fun getAccessToken(code: String): String =
        withContext(Dispatchers.IO) {
            val credentials = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())

            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_FORM_URLENCODED
                    set("Authorization", "Basic $credentials")
                }

            val body =
                LinkedMultiValueMap<String, String>().apply {
                    add("grant_type", "authorization_code")
                    add("code", code)
                    add("redirect_uri", redirectUri)
                }

            val request = HttpEntity(body, headers)

            try {
                val response =
                    restTemplate.exchange(
                        "${discordConfig.getBaseUrl()}/oauth2/token",
                        HttpMethod.POST,
                        request,
                        String::class.java,
                    )

                if (response.statusCode == HttpStatus.OK) {
                    val tokenResponse = objectMapper.readValue(response.body, TokenResponse::class.java)
                    tokenResponse.accessToken
                } else {
                    logger.warn("디스코드 토큰 교환 실패: status=${response.statusCode}, body=${response.body}")
                    throw BadRequestException("디스코드 액세스 토큰 획득에 실패했습니다")
                }
            } catch (e: Exception) {
                logger.error("디스코드 액세스 토큰 획득 실패: code=$code", e)
                throw BadRequestException("디스코드 액세스 토큰 획득에 실패했습니다")
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
                        "${discordConfig.getBaseUrl()}/users/@me",
                        HttpMethod.GET,
                        request,
                        String::class.java,
                    )

                if (response.statusCode == HttpStatus.OK) {
                    val userResponse = objectMapper.readValue(response.body, UserResponse::class.java)
                    userResponse.id
                } else {
                    logger.warn("디스코드 사용자 정보 조회 실패: status=${response.statusCode}, body=${response.body}")
                    throw BadRequestException("디스코드 사용자 정보 조회에 실패했습니다")
                }
            } catch (e: Exception) {
                logger.error("디스코드 사용자 정보 조회 실패", e)
                throw BadRequestException("디스코드 사용자 정보 조회에 실패했습니다")
            }
        }

    override fun createOAuth2Url(state: String): String {
        val params =
            mapOf(
                "client_id" to clientId,
                "redirect_uri" to redirectUri,
                "response_type" to "code",
                "scope" to "identify",
                "state" to state,
            )

        val queryString =
            params.entries.joinToString("&") { (key, value) ->
                "$key=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
            }

        return "https://discord.com/oauth2/authorize?$queryString"
    }

    private data class TokenResponse(
        @field:JsonProperty("access_token")
        val accessToken: String,
    )

    private data class UserResponse(
        val id: String,
    )
}
