package com.sight.service

import com.sight.controllers.http.dto.ReportPhoneStatusRequest
import com.sight.domain.device.BatteryStatus
import org.junit.jupiter.api.BeforeEach
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

class KhlugPhoneServiceTest {
    private val restTemplate = mock<RestTemplate>()
    private lateinit var service: KhlugPhoneService

    @BeforeEach
    fun setUp() {
        service =
            KhlugPhoneService(
                webhookUrl = "https://discord.com/api/webhooks/test",
                restTemplate = restTemplate,
            )
    }

    @Test
    fun `reportPhoneStatus는 배터리 20% 이하이고 충전 중이 아닐 때 웹훅을 호출한다`() {
        // given
        val request =
            ReportPhoneStatusRequest(
                batteryPercent = 20,
                batteryStatus = BatteryStatus.NOT_CHARGING,
            )
        given(restTemplate.postForEntity(any<String>(), any<HttpEntity<*>>(), eq(String::class.java)))
            .willReturn(ResponseEntity.ok("success"))

        // when
        service.reportPhoneStatus(request)

        // then
        verify(restTemplate).postForEntity(
            eq("https://discord.com/api/webhooks/test"),
            any<HttpEntity<*>>(),
            eq(String::class.java),
        )
    }

    @Test
    fun `reportPhoneStatus는 배터리 20% 초과이면 웹훅을 호출하지 않는다`() {
        // given
        val request =
            ReportPhoneStatusRequest(
                batteryPercent = 21,
                batteryStatus = BatteryStatus.NOT_CHARGING,
            )

        // when
        service.reportPhoneStatus(request)

        // then
        verify(restTemplate, never()).postForEntity(any<String>(), any<HttpEntity<*>>(), eq(String::class.java))
    }

    @Test
    fun `reportPhoneStatus는 충전 중이면 웹훅을 호출하지 않는다`() {
        // given
        val request =
            ReportPhoneStatusRequest(
                batteryPercent = 10,
                batteryStatus = BatteryStatus.CHARGING,
            )

        // when
        service.reportPhoneStatus(request)

        // then
        verify(restTemplate, never()).postForEntity(any<String>(), any<HttpEntity<*>>(), eq(String::class.java))
    }

    @Test
    fun `reportPhoneStatus는 웹훅 실패시 예외를 던지지 않는다`() {
        // given
        val request =
            ReportPhoneStatusRequest(
                batteryPercent = 20,
                batteryStatus = BatteryStatus.NOT_CHARGING,
            )
        given(restTemplate.postForEntity(any<String>(), any<HttpEntity<*>>(), eq(String::class.java)))
            .willThrow(RuntimeException("Webhook failed"))

        // when & then (예외가 발생하지 않으면 성공)
        service.reportPhoneStatus(request)
    }

    @Test
    fun `reportPhoneStatus는 웹훅 URL이 비어있으면 호출하지 않는다`() {
        // given
        val emptyWebhookService =
            KhlugPhoneService(
                webhookUrl = "",
                restTemplate = restTemplate,
            )
        val request =
            ReportPhoneStatusRequest(
                batteryPercent = 15,
                batteryStatus = BatteryStatus.NOT_CHARGING,
            )

        // when
        emptyWebhookService.reportPhoneStatus(request)

        // then
        verify(restTemplate, never()).postForEntity(any<String>(), any<HttpEntity<*>>(), eq(String::class.java))
    }
}
