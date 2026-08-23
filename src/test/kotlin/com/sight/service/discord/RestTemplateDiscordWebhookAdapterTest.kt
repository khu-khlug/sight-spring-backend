package com.sight.service.discord

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.web.client.RestTemplate

class RestTemplateDiscordWebhookAdapterTest {
    private val restTemplate: RestTemplate = mock()

    @Test
    fun `시스템 알림 Webhook URL이 비어 있으면 전송을 건너뛴다`() {
        val adapter = RestTemplateDiscordWebhookAdapter("", restTemplate)

        adapter.sendSystemAlert(mapOf("content" to "알림"))

        verifyNoInteractions(restTemplate)
    }
}
