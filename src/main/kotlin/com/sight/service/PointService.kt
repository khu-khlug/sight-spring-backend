package com.sight.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

data class PointRequest(
    val message: String,
    val point: Int,
    val targetUserId: Long,
)

@Service
class PointService(
    private val restTemplate: RestTemplate,
    @Value("\${auth.service.endpoint}") private val authServiceEndpoint: String,
    @Value("\${auth.service.api-key}") private val authServiceApiKey: String,
) {
    private val logger = LoggerFactory.getLogger(PointService::class.java)

    fun givePoint(
        targetUserId: Long,
        point: Int,
        message: String,
    ) {
        try {
            val headers =
                HttpHeaders().apply {
                    set("Content-Type", "application/json")
                    set("x-api-key", authServiceApiKey)
                }

            val body =
                PointRequest(
                    message = message,
                    point = point,
                    targetUserId = targetUserId,
                )

            val entity = HttpEntity(body, headers)

            restTemplate.exchange(
                "$authServiceEndpoint/internal/point",
                HttpMethod.POST,
                entity,
                Void::class.java,
            )
        } catch (e: RestClientException) {
            logger.error("포인트 지급 실패: targetUserId=$targetUserId, point=$point, message=$message", e)
        }
    }
}
