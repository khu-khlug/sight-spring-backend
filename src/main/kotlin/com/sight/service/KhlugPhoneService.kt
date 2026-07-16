package com.sight.service

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

    fun reportPhoneStatus(
        batteryPercent: Int,
        batteryStatus: BatteryStatus,
    ) {
        if (batteryPercent > 20 || batteryStatus == BatteryStatus.CHARGING) {
            return
        }

        try {
            val payload = createDiscordWebhookPayload(batteryPercent, batteryStatus)
            sendWebhook(payload)
        } catch (e: Exception) {
            logger.error("쿠러그 폰 상태 알림 전송 실패", e)
        }
    }

    fun forwardNotification(
        appName: String,
        title: String,
        content: String,
        receivedAt: Instant,
    ) {
        try {
            val payload = createNotificationWebhookPayload(appName, title, content, receivedAt)
            sendWebhook(payload)
        } catch (e: Exception) {
            logger.error("푸시 알림 포워딩 실패", e)
        }
    }

    private fun sendWebhook(payload: Map<String, Any>) {
        if (webhookUrl.isBlank()) {
            logger.warn("웹훅 URL이 설정되지 않았습니다")
            return
        }

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        val httpEntity = HttpEntity(payload, headers)

        restTemplate.postForEntity(webhookUrl, httpEntity, String::class.java)
    }

    private fun createNotificationWebhookPayload(
        appName: String,
        title: String,
        content: String,
        receivedAt: Instant,
    ): Map<String, Any> {
        val description =
            listOf(
                "**$title**",
                content,
            ).joinToString("\n")

        val embed =
            mapOf(
                "title" to "📱 $appName 알림",
                "description" to description,
                "color" to 0x3498DB,
                "timestamp" to receivedAt.toString(),
            )

        return mapOf("embeds" to listOf(embed))
    }

    private fun createDiscordWebhookPayload(
        batteryPercent: Int,
        batteryStatus: BatteryStatus,
    ): Map<String, Any> {
        val color =
            when {
                // Red
                batteryPercent <= 20 -> 0xE74C3C

                // Orange
                batteryPercent <= 50 -> 0xF39C12

                // Green
                else -> 0x2ECC71
            }

        val statusEmoji =
            when (batteryStatus) {
                BatteryStatus.CHARGING -> "🔌"
                BatteryStatus.NOT_CHARGING -> "🔋"
            }

        val statusText =
            when (batteryStatus) {
                BatteryStatus.CHARGING -> "충전 중"
                BatteryStatus.NOT_CHARGING -> "충전 안 함"
            }

        val batterySection =
            listOf(
                "**🔋 배터리**",
                "$statusEmoji **$batteryPercent%** - $statusText",
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
