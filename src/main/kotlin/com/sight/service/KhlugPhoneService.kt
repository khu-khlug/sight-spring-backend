package com.sight.service

import com.sight.controllers.http.dto.ReportPhoneStatusRequest
import com.sight.domain.device.BatteryStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.Instant

@Service
class KhlugPhoneService(
    @param:Value("\${khlug.phone.webhook-url:}")
    private val webhookUrl: String,
    @param:Qualifier("discordRestTemplate")
    private val restTemplate: RestTemplate,
) {
    private val logger = LoggerFactory.getLogger(KhlugPhoneService::class.java)

    fun reportPhoneStatus(request: ReportPhoneStatusRequest) {
        if (webhookUrl.isBlank()) {
            logger.warn("웹훅 URL이 설정되지 않았습니다")
            return
        }

        try {
            val webhookPayload = createDiscordWebhookPayload(request)
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                }
            val httpEntity = HttpEntity(webhookPayload, headers)

            restTemplate.postForEntity(webhookUrl, httpEntity, String::class.java)
        } catch (e: Exception) {
            // Fire-and-forget: 로그만 남기고 예외를 던지지 않음
            logger.error("쿠러그 폰 상태 알림 전송 실패", e)
        }
    }

    private fun createDiscordWebhookPayload(request: ReportPhoneStatusRequest): Map<String, Any> {
        val color =
            when {
                // Red
                request.batteryPercent <= 20 -> 0xE74C3C

                // Orange
                request.batteryPercent <= 50 -> 0xF39C12

                // Green
                else -> 0x2ECC71
            }

        val statusEmoji =
            when (request.batteryStatus) {
                BatteryStatus.CHARGING -> "🔌"
                BatteryStatus.NOT_CHARGING -> "🔋"
            }

        val statusText =
            when (request.batteryStatus) {
                BatteryStatus.CHARGING -> "충전 중"
                BatteryStatus.NOT_CHARGING -> "충전 안 함"
            }

        val batterySection =
            listOf(
                "**🔋 배터리**",
                "$statusEmoji **${request.batteryPercent}%** - $statusText",
            ).joinToString("\n")

        val embed =
            mapOf(
                "title" to "📱 쿠러그 공용 폰 상태",
                "description" to batterySection,
                "color" to color,
                "timestamp" to Instant.now().toString(),
            )

        return mapOf("embeds" to listOf(embed))
    }
}
