package com.sight.domain.sms

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SmsMessageTest {
    @Test
    fun `ASCII 90바이트 메시지는 SMS이고 91바이트 메시지는 LMS이다`() {
        assertEquals(SmsMessageType.SMS, SmsMessage.personalize("a".repeat(90), "수신자").type)
        assertEquals(SmsMessageType.LMS, SmsMessage.personalize("a".repeat(91), "수신자").type)
    }

    @Test
    fun `한글 45자는 SMS이고 46자는 LMS이다`() {
        assertEquals(SmsMessageType.SMS, SmsMessage.personalize("가".repeat(45), "수신자").type)
        assertEquals(SmsMessageType.LMS, SmsMessage.personalize("가".repeat(46), "수신자").type)
    }

    @Test
    fun `수신자 이름을 치환한 최종 메시지로 문자 유형을 결정한다`() {
        val message = SmsMessage.personalize("a".repeat(88) + "{realname}", "가")

        assertEquals("a".repeat(88) + "가", message.text)
        assertEquals(SmsMessageType.SMS, message.type)

        val longerMessage = SmsMessage.personalize("a".repeat(89) + "{realname}", "가")
        assertEquals(SmsMessageType.LMS, longerMessage.type)
    }

    @Test
    fun `전화번호는 ASCII 숫자만 남겨 정규화한다`() {
        assertEquals("01012345678", PhoneNumberNormalizer.normalize("010-1234 (5678)"))
        assertEquals("", PhoneNumberNormalizer.normalize("전화번호 없음"))
    }
}
