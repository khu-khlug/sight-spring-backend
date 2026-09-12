package com.sight.service

import com.sight.core.config.ConfigKey
import com.sight.core.config.SystemConfigRegistry
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.InternalServerErrorException
import com.sight.core.exception.NotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertEquals

class SenderPhoneServiceTest {
    private val systemConfigRegistry = mock<SystemConfigRegistry>()
    private lateinit var senderPhoneService: SenderPhoneService

    @BeforeEach
    fun setUp() {
        senderPhoneService = SenderPhoneService(systemConfigRegistry)
    }

    @Test
    fun `공식 발신번호는 캐시를 사용하지 않고 최신 값을 조회한다`() {
        given(systemConfigRegistry.getFreshValue(ConfigKey.SMS_SENDER_PHONE)).willReturn("029302266")

        val result = senderPhoneService.getSenderPhone()

        assertEquals("029302266", result)
        verify(systemConfigRegistry).getFreshValue(ConfigKey.SMS_SENDER_PHONE)
    }

    @Test
    fun `공식 발신번호가 없으면 조회 요청에 NotFoundException을 던진다`() {
        given(systemConfigRegistry.getFreshValue(ConfigKey.SMS_SENDER_PHONE)).willReturn("")

        assertThrows<NotFoundException> {
            senderPhoneService.getSenderPhone()
        }
    }

    @Test
    fun `문자 발송에 사용할 공식 발신번호가 없으면 InternalServerErrorException을 던진다`() {
        given(systemConfigRegistry.getFreshValue(ConfigKey.SMS_SENDER_PHONE)).willReturn("")

        assertThrows<InternalServerErrorException> {
            senderPhoneService.getSenderPhoneForSending()
        }
    }

    @Test
    fun `공식 발신번호는 숫자만 남겨 저장한다`() {
        senderPhoneService.updateSenderPhone("02-930-2266")

        verify(systemConfigRegistry).setValue(ConfigKey.SMS_SENDER_PHONE, "029302266")
    }

    @Test
    fun `공식 발신번호에 숫자가 없으면 BadRequestException을 던진다`() {
        assertThrows<BadRequestException> {
            senderPhoneService.updateSenderPhone("전화번호 없음")
        }
    }
}
