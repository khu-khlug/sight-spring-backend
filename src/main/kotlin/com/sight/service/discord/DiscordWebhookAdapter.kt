package com.sight.service.discord

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import org.springframework.web.util.UriComponentsBuilder

interface DiscordWebhookAdapter {
    suspend fun sendSystemAlert(payload: Map<String, Any>)

    suspend fun sendDoorLockAlert(payload: Map<String, Any>)
}

@Component
class WebClientDiscordWebhookAdapter(
    @param:Value("\${discord.webhook.system-alert-url:}")
    private val systemAlertWebhookUrl: String,
    @param:Value("\${discord.webhook.door-lock-alert-url:}")
    private val doorLockAlertWebhookUrl: String,
    @param:Qualifier("discordWebClient")
    private val webClient: WebClient,
) : DiscordWebhookAdapter {
    private val logger = LoggerFactory.getLogger(WebClientDiscordWebhookAdapter::class.java)

    override suspend fun sendSystemAlert(payload: Map<String, Any>) {
        if (systemAlertWebhookUrl.isBlank()) {
            return
        }

        val webhookUrl =
            UriComponentsBuilder
                .fromUriString(systemAlertWebhookUrl)
                .queryParam("with_components", true)
                .build()
                .toUriString()

        postWebhook(webhookUrl, payload)
    }

    override suspend fun sendDoorLockAlert(payload: Map<String, Any>) {
        if (doorLockAlertWebhookUrl.isBlank()) {
            logger.warn("도어락 알림 웹훅 URL이 설정되지 않았습니다")
            return
        }

        postWebhook(doorLockAlertWebhookUrl, payload)
    }

    private suspend fun postWebhook(
        url: String,
        payload: Map<String, Any>,
    ) {
        webClient
            .post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .awaitBodilessEntity()
    }
}
