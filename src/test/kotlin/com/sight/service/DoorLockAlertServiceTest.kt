package com.sight.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class DoorLockAlertServiceTest {
    private val restTemplate: RestTemplate = mock()

    @Test
    fun `alertDie는 디스코드 웹훅으로 알림을 전송한다`() {
        val webhookUrl = "https://discord.com/api/webhooks/door-lock"
        val service = DoorLockAlertService(webhookUrl, restTemplate)
        given(restTemplate.postForEntity(any<String>(), any<HttpEntity<*>>(), eq(String::class.java)))
            .willReturn(ResponseEntity.ok("success"))

        service.alertDie()

        verify(restTemplate).postForEntity(
            eq(webhookUrl),
            any<HttpEntity<*>>(),
            eq(String::class.java),
        )
    }

    @Test
    fun `alertDie는 웹훅 URL이 비어있으면 전송하지 않는다`() {
        val service = DoorLockAlertService("", restTemplate)

        service.alertDie()

        verify(restTemplate, never()).postForEntity(any<String>(), any<HttpEntity<*>>(), eq(String::class.java))
    }

    @Test
    fun `alertDie는 웹훅 전송 실패시 예외를 던지지 않는다`() {
        val service = DoorLockAlertService("https://discord.com/api/webhooks/door-lock", restTemplate)
        given(restTemplate.postForEntity(any<String>(), any<HttpEntity<*>>(), eq(String::class.java)))
            .willThrow(RuntimeException("Webhook failed"))

        service.alertDie()
    }
}
