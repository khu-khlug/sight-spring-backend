package com.sight.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class DoorLockAlertService(
    @param:Value("\${khlug.door-lock.webhook-url:}")
    private val webhookUrl: String,
    @param:Qualifier("discordRestTemplate")
    private val restTemplate: RestTemplate,
) {
    private val logger = LoggerFactory.getLogger(DoorLockAlertService::class.java)

    fun alertDie() {
        if (webhookUrl.isBlank()) {
            logger.warn("도어락 알림 웹훅 URL이 설정되지 않았습니다")
            return
        }

        try {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                }
            val payload = mapOf("content" to ALERT_MESSAGE)

            restTemplate.postForEntity(
                webhookUrl,
                HttpEntity(payload, headers),
                String::class.java,
            )
        } catch (e: Exception) {
            logger.error("도어락 데몬 응답 없음 알림 전송 실패", e)
        }
    }

    companion object {
        private const val ALERT_MESSAGE = "🔴 도어락 데몬 응답 없음 - 라즈베리파이 또는 Flask 데몬 상태를 확인하세요."
    }
}
