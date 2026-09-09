package com.sight.service.discord

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.http.HttpEntity
import org.springframework.web.client.RestTemplate

class RestTemplateDiscordWebhookAdapterTest {
    private val restTemplate: RestTemplate = mock()

    @Test
    fun `시스템 알림 Webhook URL이 비어 있으면 전송을 건너뛴다`() {
        val adapter = RestTemplateDiscordWebhookAdapter("", restTemplate)

        adapter.sendSystemAlert(mapOf("content" to "알림"))

        verifyNoInteractions(restTemplate)
    }

    @Test
    fun `시스템 알림 전송 URL에 components 활성화 파라미터를 포함한다`() {
        val adapter = RestTemplateDiscordWebhookAdapter("https://discord.com/api/webhooks/id/token?wait=true", restTemplate)

        adapter.sendSystemAlert(mapOf("content" to "알림"))

        verify(restTemplate).postForEntity(
            eq("https://discord.com/api/webhooks/id/token?wait=true&with_components=true"),
            any<HttpEntity<Map<String, Any>>>(),
            eq(String::class.java),
        )
    }
}
