package com.sight.service

import com.sight.domain.auth.Requester
import com.sight.domain.auth.UserRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

data class AuthResponse(
    val login: Boolean,
    val userId: Long? = null,
)

@Service
class AuthService(
    private val restTemplate: RestTemplate,
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

    // TODO: Replace with proper User entity and repository
    fun getUserRole(userId: Long): UserRole {
        // Temporary implementation - replace with actual database query
        return if (userId == 123L) UserRole.MANAGER else UserRole.USER
    }

    fun createRequester(userId: Long): Requester {
        val role = getUserRole(userId)
        return Requester(userId, role)
    }
}
