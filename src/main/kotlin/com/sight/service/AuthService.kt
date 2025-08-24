package com.sight.service

import com.sight.domain.auth.Requester
import com.sight.domain.auth.UserRole
import com.sight.repository.MemberRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException

data class AuthResponse(
    val login: Boolean,
    val userId: Long? = null,
)

@Service
class AuthService(
    private val restTemplate: RestTemplate,
    private val memberRepository: MemberRepository,
    @Value("\${auth.service.endpoint}") private val authServiceEndpoint: String,
    @Value("\${auth.service.api-key}") private val authServiceApiKey: String,
) {
    fun authenticate(cookies: String): Long? {
        return try {
            val headers =
                HttpHeaders().apply {
                    set("Cookie", cookies)
                    set("Content-Type", "application/json")
                    set("x-api-key", authServiceApiKey)
                }

            val entity = HttpEntity<Map<String, Any>>(emptyMap(), headers)

            val response =
                restTemplate.exchange(
                    "$authServiceEndpoint/internal/auth",
                    HttpMethod.POST,
                    entity,
                    AuthResponse::class.java,
                )

            response.body?.let { authResponse ->
                if (authResponse.login) authResponse.userId else null
            }
        } catch (e: RestClientException) {
            null
        }
    }

    fun getUserRole(userId: Long): UserRole {
        val member =
            memberRepository.findById(userId).orElse(null)
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")

        return if (member.manager) UserRole.MANAGER else UserRole.USER
    }

    fun createRequester(userId: Long): Requester {
        val role = getUserRole(userId)
        return Requester(userId, role)
    }
}
