package com.sight.service.discord

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

interface DiscordWebhookAdapter {
    fun sendSystemAlert(payload: Map<String, Any>)
}

@Component
class RestTemplateDiscordWebhookAdapter(
    @param:Value("\${discord.webhook.system-alert-url:}")
    private val systemAlertWebhookUrl: String,
    @param:Qualifier("discordRestTemplate")
    private val restTemplate: RestTemplate,
) : DiscordWebhookAdapter {
    override fun sendSystemAlert(payload: Map<String, Any>) {
        if (systemAlertWebhookUrl.isBlank()) {
            return
        }

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        restTemplate.postForEntity(systemAlertWebhookUrl, HttpEntity(payload, headers), String::class.java)
    }
}
