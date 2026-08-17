package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.InternalServerErrorException
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.domain.notification.NotificationCategory
import com.sight.domain.sms.SmsMessageType
import com.sight.repository.MemberRepository
import com.sight.service.sms.SendSmsMessage
import com.sight.service.sms.SmsMessageClient
import com.sight.service.sms.SmsMessageClientException
import com.sight.service.sms.SmsMessageClientResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmsMessageServiceTest {
    private val memberRepository = mock<MemberRepository>()
    private val senderPhoneService = mock<SenderPhoneService>()
    private val smsMessageClient = mock<SmsMessageClient>()
    private val notificationService = mock<NotificationService>()
    private lateinit var smsMessageService: SmsMessageService

    @BeforeEach
    fun setUp() {
        smsMessageService = SmsMessageService(memberRepository, senderPhoneService, smsMessageClient, notificationService)
    }

    @Test
    fun `회원과 직접 지정 수신자를 정규화하고 중복 제거 후 요청 순서로 발송한다`() {
        val firstMember = member(id = 2L, realname = "김쿠러그", phone = "010-1111-2222")
        val duplicatedPhoneMember = member(id = 1L, realname = "이쿠러그", phone = "01011112222")
        given(memberRepository.findAllById(listOf(2L, 1L))).willReturn(listOf(duplicatedPhoneMember, firstMember))
        given(senderPhoneService.getSenderPhoneForSending()).willReturn("029302266")
        given(smsMessageClient.send(any())).willAnswer { invocation ->
            invocation.getArgument<List<SendSmsMessage>>(0).map {
                SmsMessageClientResult(recipientIndex = it.recipientIndex, accepted = true)
            }
        }
        val messagesCaptor = argumentCaptor<List<SendSmsMessage>>()

        val result =
            smsMessageService.sendSmsMessages(
                memberIds = listOf(2L, 1L, 2L),
                additionalPhoneNumbers = listOf("010-1111-2222, 010-3333-4444", "번호 없음"),
                message = "안녕하세요 {realname}",
            )

        verify(smsMessageClient).send(messagesCaptor.capture())
        assertEquals(2, messagesCaptor.firstValue.size)
        assertEquals("01011112222", messagesCaptor.firstValue[0].to)
        assertEquals("안녕하세요 김쿠러그", messagesCaptor.firstValue[0].text)
        assertEquals("01033334444", messagesCaptor.firstValue[1].to)
        assertEquals("안녕하세요 01033334444", messagesCaptor.firstValue[1].text)
        assertEquals(listOf(2L, null), result.results.map { it.memberId })
        assertEquals(listOf("01011112222", "01033334444"), result.results.map { it.phone })
        assertTrue(result.results.all { it.status == SmsMessageResultStatus.SENT })
        assertFalse(result.hasFailures)
        verify(notificationService).createNotificationForManagers(
            NotificationCategory.SYSTEM,
            "",
            "<u>김쿠러그</u> 회원, <u>010-3333-4444</u>에게 <u>안녕하세요 {realname}</u> 내용으로 문자를 발송했습니다.",
        )
    }

    @Test
    fun `허용되지 않는 회원이 하나라도 있으면 전체 요청을 거부한다`() {
        val unitedMember = member(id = 1L, studentStatus = StudentStatus.UNITED)
        given(memberRepository.findAllById(listOf(1L, 999L))).willReturn(listOf(unitedMember))

        assertThrows<BadRequestException> {
            smsMessageService.sendSmsMessages(
                memberIds = listOf(1L, 999L),
                additionalPhoneNumbers = listOf("01012345678"),
                message = "안내 메시지",
            )
        }
        verify(senderPhoneService, never()).getSenderPhoneForSending()
        verify(smsMessageClient, never()).send(any())
    }

    @Test
    fun `차단 상태 회원은 현재 회원 수신자로 발송한다`() {
        val inactiveMember = member(id = 1L, status = UserStatus.INACTIVE)
        given(memberRepository.findAllById(listOf(1L))).willReturn(listOf(inactiveMember))
        given(senderPhoneService.getSenderPhoneForSending()).willReturn("029302266")
        given(smsMessageClient.send(any())).willReturn(
            listOf(SmsMessageClientResult(recipientIndex = "0", accepted = true)),
        )

        val result =
            smsMessageService.sendSmsMessages(
                memberIds = listOf(1L),
                additionalPhoneNumbers = emptyList(),
                message = "안내 메시지",
            )

        assertEquals(SmsMessageResultStatus.SENT, result.results.single().status)
        verify(smsMessageClient).send(any())
    }

    @Test
    fun `전화번호가 없는 회원만 지정하면 외부 발송 없이 SKIPPED 결과를 반환한다`() {
        val member = member(id = 1L, phone = "전화번호 없음")
        given(memberRepository.findAllById(listOf(1L))).willReturn(listOf(member))

        val result =
            smsMessageService.sendSmsMessages(
                memberIds = listOf(1L),
                additionalPhoneNumbers = emptyList(),
                message = "안내 메시지",
            )

        assertTrue(result.hasFailures)
        assertEquals(SmsMessageResultStatus.SKIPPED, result.results.single().status)
        assertNull(result.results.single().phone)
        assertNull(result.results.single().type)
        verify(senderPhoneService, never()).getSenderPhoneForSending()
        verify(smsMessageClient, never()).send(any())
    }

    @Test
    fun `유효한 회원과 직접 지정 전화번호가 없으면 요청을 거부한다`() {
        assertThrows<BadRequestException> {
            smsMessageService.sendSmsMessages(
                memberIds = emptyList(),
                additionalPhoneNumbers = listOf("번호 없음", "---"),
                message = "안내 메시지",
            )
        }
        verify(smsMessageClient, never()).send(any())
    }

    @Test
    fun `공백 메시지는 회원 조회와 외부 발송 전에 거부한다`() {
        assertThrows<BadRequestException> {
            smsMessageService.sendSmsMessages(
                memberIds = listOf(1L),
                additionalPhoneNumbers = emptyList(),
                message = "   ",
            )
        }
        verify(memberRepository, never()).findAllById(any<List<Long>>())
        verify(smsMessageClient, never()).send(any())
    }

    @Test
    fun `SOLAPI 수신자별 결과를 원래 수신자 순서로 반환한다`() {
        given(memberRepository.findAllById(emptyList())).willReturn(emptyList())
        given(senderPhoneService.getSenderPhoneForSending()).willReturn("029302266")
        given(smsMessageClient.send(any())).willReturn(
            listOf(
                SmsMessageClientResult(recipientIndex = "1", accepted = false),
                SmsMessageClientResult(recipientIndex = "0", accepted = true),
            ),
        )

        val result =
            smsMessageService.sendSmsMessages(
                memberIds = emptyList(),
                additionalPhoneNumbers = listOf("01011112222", "01033334444"),
                message = "안내 메시지",
            )

        assertEquals(SmsMessageResultStatus.SENT, result.results[0].status)
        assertNull(result.results[0].message)
        assertEquals(SmsMessageResultStatus.FAILED, result.results[1].status)
        assertEquals("문자 발송 서비스가 수신자를 거부했습니다", result.results[1].message)
        assertTrue(result.hasFailures)
    }

    @Test
    fun `치환 결과에 따라 수신자별 SMS와 LMS 유형을 전달한다`() {
        val shortNameMember = member(id = 1L, realname = "가", phone = "01011112222")
        val longNameMember = member(id = 2L, realname = "가가", phone = "01033334444")
        given(memberRepository.findAllById(listOf(1L, 2L))).willReturn(listOf(shortNameMember, longNameMember))
        given(senderPhoneService.getSenderPhoneForSending()).willReturn("029302266")
        given(smsMessageClient.send(any())).willAnswer { invocation ->
            invocation.getArgument<List<SendSmsMessage>>(0).map {
                SmsMessageClientResult(recipientIndex = it.recipientIndex, accepted = true)
            }
        }
        val messagesCaptor = argumentCaptor<List<SendSmsMessage>>()

        smsMessageService.sendSmsMessages(
            memberIds = listOf(1L, 2L),
            additionalPhoneNumbers = emptyList(),
            message = "a".repeat(88) + "{realname}",
        )

        verify(smsMessageClient).send(messagesCaptor.capture())
        assertEquals(SmsMessageType.SMS, messagesCaptor.firstValue[0].type)
        assertEquals(SmsMessageType.LMS, messagesCaptor.firstValue[1].type)
    }

    @Test
    fun `SOLAPI 요청 자체가 실패하면 InternalServerErrorException을 던진다`() {
        given(memberRepository.findAllById(emptyList())).willReturn(emptyList())
        given(senderPhoneService.getSenderPhoneForSending()).willReturn("029302266")
        given(smsMessageClient.send(any())).willThrow(SmsMessageClientException("문자 발송 서비스와 통신할 수 없습니다"))

        val exception =
            assertThrows<InternalServerErrorException> {
                smsMessageService.sendSmsMessages(
                    memberIds = emptyList(),
                    additionalPhoneNumbers = listOf("01011112222"),
                    message = "안내 메시지",
                )
            }

        assertEquals("문자 발송 서비스와 통신할 수 없습니다", exception.message)
        verify(notificationService, never()).createNotificationForManagers(
            any<NotificationCategory>(),
            any<String>(),
            any<String>(),
            anyOrNull(),
        )
    }

    private fun member(
        id: Long,
        realname: String = "회원$id",
        phone: String? = "01012345678",
        studentStatus: StudentStatus = StudentStatus.UNDERGRADUATE,
        status: UserStatus = UserStatus.ACTIVE,
    ): Member =
        Member(
            id = id,
            name = "member$id",
            realname = realname,
            phone = phone,
            studentStatus = studentStatus,
            status = status,
        )
}
