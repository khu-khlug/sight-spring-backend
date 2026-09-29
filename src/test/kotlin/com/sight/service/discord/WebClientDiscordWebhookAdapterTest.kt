package com.sight.service.discord

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class WebClientDiscordWebhookAdapterTest {
    private val webClient: WebClient = mock()
    private val requestBodyUriSpec: WebClient.RequestBodyUriSpec = mock()
    private val requestBodySpec: WebClient.RequestBodySpec = mock()
    private val requestHeadersSpec: WebClient.RequestHeadersSpec<*> = mock()
    private val responseSpec: WebClient.ResponseSpec = mock()

    private fun stubSuccessfulPost(uri: String) {
        given(webClient.post()).willReturn(requestBodyUriSpec)
        given(requestBodyUriSpec.uri(eq(uri))).willReturn(requestBodySpec)
        given(requestBodySpec.contentType(eq(MediaType.APPLICATION_JSON))).willReturn(requestBodySpec)
        given(requestBodySpec.bodyValue(any<Map<String, Any>>())).willReturn(requestHeadersSpec)
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec)
        given(responseSpec.toBodilessEntity())
            .willReturn(Mono.just(ResponseEntity.noContent().build()))
    }

    @Test
    fun `시스템 알림 Webhook URL이 비어 있으면 전송을 건너뛴다`() {
        val adapter = WebClientDiscordWebhookAdapter("", "https://discord.com/api/webhooks/door-lock", webClient)

        runBlocking { adapter.sendSystemAlert(mapOf("content" to "알림")) }

        verifyNoInteractions(webClient)
    }

    @Test
    fun `시스템 알림 전송 URL에 components 활성화 파라미터를 포함한다`() {
        val expectedUri = "https://discord.com/api/webhooks/id/token?wait=true&with_components=true"
        stubSuccessfulPost(expectedUri)
        val adapter =
            WebClientDiscordWebhookAdapter(
                "https://discord.com/api/webhooks/id/token?wait=true",
                "",
                webClient,
            )

        runBlocking { adapter.sendSystemAlert(mapOf("content" to "알림")) }

        verify(requestBodyUriSpec).uri(eq(expectedUri))
    }

    @Test
    fun `도어락 알림 Webhook URL이 비어 있으면 전송을 건너뛴다`() {
        val adapter = WebClientDiscordWebhookAdapter("https://discord.com/api/webhooks/system", "", webClient)

        runBlocking { adapter.sendDoorLockAlert(mapOf("content" to "알림")) }

        verifyNoInteractions(webClient)
    }

    @Test
    fun `도어락 알림은 파라미터 없이 그대로 전송한다`() {
        val doorLockUrl = "https://discord.com/api/webhooks/door-lock"
        stubSuccessfulPost(doorLockUrl)
        val adapter = WebClientDiscordWebhookAdapter("", doorLockUrl, webClient)

        runBlocking { adapter.sendDoorLockAlert(mapOf("content" to "알림")) }

        verify(requestBodyUriSpec).uri(eq(doorLockUrl))
    }
}
