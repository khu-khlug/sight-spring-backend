package com.sight.service

import com.sight.core.exception.ConflictException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.discord.DiscordIntegration
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.domain.notification.Notification
import com.sight.domain.notification.NotificationCategory
import com.sight.domain.supportrequest.SupportRequest
import com.sight.domain.supportrequest.SupportRequestCategory
import com.sight.domain.supportrequest.SupportRequestComment
import com.sight.repository.DiscordIntegrationRepository
import com.sight.repository.MemberRepository
import com.sight.repository.SupportRequestCommentRepository
import com.sight.repository.SupportRequestRepository
import com.sight.service.discord.DiscordApiAdapter
import com.sight.service.discord.DiscordWebhookAdapter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals

class SupportRequestServiceTest {
    private val supportRequestRepository: SupportRequestRepository = mock()
    private val supportRequestCommentRepository: SupportRequestCommentRepository = mock()
    private val memberRepository: MemberRepository = mock()
    private val notificationService: NotificationService = mock()
    private val discordIntegrationRepository: DiscordIntegrationRepository = mock()
    private val discordApiAdapter: DiscordApiAdapter = mock()
    private val discordWebhookAdapter: DiscordWebhookAdapter = mock()
    private lateinit var supportRequestService: SupportRequestService

    @BeforeEach
    fun setUp() {
        supportRequestService =
            SupportRequestService(
                supportRequestRepository,
                supportRequestCommentRepository,
                memberRepository,
                notificationService,
                discordIntegrationRepository,
                discordApiAdapter,
                discordWebhookAdapter,
            )
    }

    @Test
    fun `지원 신청을 생성하면 운영진 알림과 Discord 시스템 알림 Webhook 전송을 수행한다`() {
        val requester = member(id = 10L, realname = "신청자")
        given(memberRepository.findById(10L)).willReturn(Optional.of(requester))
        given(supportRequestRepository.save(any<SupportRequest>())).willAnswer { it.arguments[0] }
        given(notificationService.createNotificationForManagers(any(), any(), any(), anyOrNull()))
            .willReturn(emptyList())

        val result =
            supportRequestService.createSupportRequest(
                requesterId = 10L,
                category = SupportRequestCategory.SERVER_SPACE,
                title = "서버 공간",
                content = "프로젝트 서버가 필요합니다",
            )

        assertEquals(10L, result.requester.userId)
        assertEquals("신청자", result.requester.name)
        assertEquals(false, result.hasComments)
        verify(notificationService).createNotificationForManagers(
            NotificationCategory.SYSTEM,
            "새 지원 신청",
            "서버 공간 지원 신청이 등록되었습니다.",
            "/support/${result.supportRequest.id}",
        )
        verify(discordWebhookAdapter).sendSystemAlert(
            mapOf(
                "embeds" to
                    listOf(
                        mapOf(
                            "title" to "새 지원 신청",
                            "description" to
                                listOf(
                                    "지원 신청 ID: ${result.supportRequest.id}",
                                    "카테고리: SERVER_SPACE",
                                    "제목: 서버 공간",
                                    "신청자: 신청자",
                                ).joinToString("\n"),
                        ),
                    ),
            ),
        )
    }

    @Test
    fun `같은 지원 신청을 반복 생성하면 각각 별도 리소스를 저장한다`() {
        val requester = member(id = 10L)
        given(memberRepository.findById(10L)).willReturn(Optional.of(requester))
        given(supportRequestRepository.save(any<SupportRequest>())).willAnswer { it.arguments[0] }
        given(notificationService.createNotificationForManagers(any(), any(), any(), anyOrNull()))
            .willReturn(emptyList())

        val first = supportRequestService.createSupportRequest(10L, SupportRequestCategory.OTHER, "제목", "내용")
        val second = supportRequestService.createSupportRequest(10L, SupportRequestCategory.OTHER, "제목", "내용")

        assert(first.supportRequest.id != second.supportRequest.id)
        verify(supportRequestRepository, org.mockito.kotlin.times(2)).save(any<SupportRequest>())
    }

    @Test
    fun `첫 댓글이 등록된 지원 신청은 신청자가 수정할 수 없다`() {
        val supportRequest = supportRequest()
        given(supportRequestRepository.findByIdForUpdate(supportRequest.id)).willReturn(supportRequest)
        given(supportRequestCommentRepository.existsBySupportRequestId(supportRequest.id)).willReturn(true)

        assertThrows<ConflictException> {
            supportRequestService.updateSupportRequest(
                supportRequest.id,
                requesterId = supportRequest.requesterId,
                category = SupportRequestCategory.BOOK,
                title = "변경 제목",
                content = "변경 내용",
            )
        }
        assertEquals("기존 제목", supportRequest.title)
    }

    @Test
    fun `첫 댓글 전 신청자는 자신의 지원 신청을 수정할 수 있다`() {
        val supportRequest = supportRequest()
        val requester = member(id = supportRequest.requesterId)
        given(supportRequestRepository.findByIdForUpdate(supportRequest.id)).willReturn(supportRequest)
        given(supportRequestCommentRepository.existsBySupportRequestId(supportRequest.id)).willReturn(false)
        given(memberRepository.findById(supportRequest.requesterId)).willReturn(Optional.of(requester))

        val result =
            supportRequestService.updateSupportRequest(
                supportRequest.id,
                requesterId = supportRequest.requesterId,
                category = SupportRequestCategory.BOOK,
                title = "변경 제목",
                content = "변경 내용",
            )

        assertEquals(SupportRequestCategory.BOOK, result.supportRequest.category)
        assertEquals("변경 제목", result.supportRequest.title)
        assertEquals("변경 내용", result.supportRequest.content)
    }

    @Test
    fun `신청자가 아닌 회원은 지원 신청을 수정할 수 없다`() {
        val supportRequest = supportRequest()
        given(supportRequestRepository.findByIdForUpdate(supportRequest.id)).willReturn(supportRequest)

        assertThrows<NotFoundException> {
            supportRequestService.updateSupportRequest(
                supportRequest.id,
                requesterId = 99L,
                category = SupportRequestCategory.BOOK,
                title = "변경 제목",
                content = "변경 내용",
            )
        }
    }

    @Test
    fun `신청자는 자신의 지원 신청에 댓글을 작성하고 자신에게도 알림을 받는다`() {
        val supportRequest = supportRequest(requesterId = 10L)
        val requester = member(id = 10L, realname = "신청자")
        given(supportRequestRepository.findByIdForUpdate(supportRequest.id)).willReturn(supportRequest)
        given(memberRepository.findById(10L)).willReturn(Optional.of(requester))
        given(supportRequestCommentRepository.save(any<SupportRequestComment>())).willAnswer { it.arguments[0] }
        given(notificationService.createNotificationForManagers(any(), any(), any(), anyOrNull()))
            .willReturn(emptyList())

        val result = supportRequestService.createSupportRequestComment(supportRequest.id, 10L, false, "추가 설명입니다")

        assertEquals("신청자", result.author.name)
        verify(notificationService).createNotification(
            10L,
            NotificationCategory.SYSTEM,
            "지원 신청 댓글",
            "기존 제목 지원 신청에 댓글이 등록되었습니다.",
            "/support/${supportRequest.id}",
        )
        verify(discordApiAdapter, never()).sendSupportRequestCommentDirectMessage(any(), any(), any(), any())
    }

    @Test
    fun `일반 회원은 다른 회원의 지원 신청에 댓글을 작성할 수 없다`() {
        val supportRequest = supportRequest(requesterId = 10L)
        given(supportRequestRepository.findByIdForUpdate(supportRequest.id)).willReturn(supportRequest)

        assertThrows<ForbiddenException> {
            supportRequestService.createSupportRequestComment(supportRequest.id, 20L, false, "댓글")
        }
        verify(supportRequestCommentRepository, never()).save(any<SupportRequestComment>())
    }

    @Test
    fun `운영진 댓글은 신청자에게 Discord DM을 전송한다`() {
        val supportRequest = supportRequest(requesterId = 10L)
        val manager = member(id = 20L, realname = "운영진")
        given(supportRequestRepository.findByIdForUpdate(supportRequest.id)).willReturn(supportRequest)
        given(memberRepository.findById(20L)).willReturn(Optional.of(manager))
        given(supportRequestCommentRepository.save(any<SupportRequestComment>())).willAnswer { it.arguments[0] }
        given(notificationService.createNotificationForManagers(any(), any(), any(), anyOrNull()))
            .willReturn(listOf(notification(userId = 10L)))
        given(discordIntegrationRepository.findByUserId(10L))
            .willReturn(
                DiscordIntegration(
                    "discord-integration",
                    10L,
                    "discord-user",
                    Instant.now().atZone(java.time.ZoneOffset.UTC).toLocalDateTime(),
                ),
            )

        supportRequestService.createSupportRequestComment(supportRequest.id, 20L, true, "운영진 댓글")

        verify(discordApiAdapter).sendSupportRequestCommentDirectMessage(
            "discord-user",
            supportRequest.id,
            "기존 제목",
            "운영진",
        )
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `Discord 전송 실패는 지원 신청 생성을 취소하지 않는다`() {
        val requester = member(id = 10L)
        given(memberRepository.findById(10L)).willReturn(Optional.of(requester))
        given(supportRequestRepository.save(any<SupportRequest>())).willAnswer { it.arguments[0] }
        given(notificationService.createNotificationForManagers(any(), any(), any(), anyOrNull()))
            .willReturn(emptyList())
        org.mockito.kotlin.doThrow(RuntimeException("Discord timeout"))
            .`when`(discordWebhookAdapter)
            .sendSystemAlert(any())

        val result = supportRequestService.createSupportRequest(10L, SupportRequestCategory.OTHER, "제목", "내용")

        assertEquals("제목", result.supportRequest.title)
        verify(supportRequestRepository).save(any<SupportRequest>())
    }

    @Test
    fun `사이트 내 알림 생성 실패는 지원 신청 생성을 취소하지 않는다`() {
        val requester = member(id = 10L)
        given(memberRepository.findById(10L)).willReturn(Optional.of(requester))
        given(supportRequestRepository.save(any<SupportRequest>())).willAnswer { it.arguments[0] }
        org.mockito.kotlin.doThrow(RuntimeException("알림 저장 실패"))
            .`when`(notificationService)
            .createNotificationForManagers(any(), any(), any(), anyOrNull())

        val result = supportRequestService.createSupportRequest(10L, SupportRequestCategory.OTHER, "제목", "내용")

        assertEquals("제목", result.supportRequest.title)
        verify(supportRequestRepository).save(any<SupportRequest>())
    }

    @Test
    fun `같은 댓글을 반복 생성하면 각각 별도 댓글과 알림을 만든다`() {
        val supportRequest = supportRequest(requesterId = 10L)
        val requester = member(id = 10L)
        given(supportRequestRepository.findByIdForUpdate(supportRequest.id)).willReturn(supportRequest)
        given(memberRepository.findById(10L)).willReturn(Optional.of(requester))
        given(supportRequestCommentRepository.save(any<SupportRequestComment>())).willAnswer { it.arguments[0] }
        given(notificationService.createNotificationForManagers(any(), any(), any(), anyOrNull()))
            .willReturn(emptyList())

        val first = supportRequestService.createSupportRequestComment(supportRequest.id, 10L, false, "댓글")
        val second = supportRequestService.createSupportRequestComment(supportRequest.id, 10L, false, "댓글")

        assert(first.comment.id != second.comment.id)
        verify(supportRequestCommentRepository, times(2)).save(any<SupportRequestComment>())
        verify(notificationService, times(2)).createNotification(
            10L,
            NotificationCategory.SYSTEM,
            "지원 신청 댓글",
            "기존 제목 지원 신청에 댓글이 등록되었습니다.",
            "/support/${supportRequest.id}",
        )
    }

    @Test
    fun `운영진은 댓글이 있는 지원 신청도 삭제할 수 있다`() {
        val supportRequest = supportRequest()
        given(supportRequestRepository.findByIdForUpdate(supportRequest.id)).willReturn(supportRequest)

        supportRequestService.deleteSupportRequest(supportRequest.id)

        verify(supportRequestRepository).delete(supportRequest)
    }

    private fun supportRequest(
        id: String = "01JQ5J4ZAVY7YKA0GHRD33RHZG",
        requesterId: Long = 10L,
    ): SupportRequest =
        SupportRequest(
            id = id,
            requesterId = requesterId,
            category = SupportRequestCategory.SERVER_SPACE,
            title = "기존 제목",
            content = "기존 내용",
        )

    private fun notification(userId: Long): Notification =
        Notification(
            id = "notification-$userId",
            userId = userId,
            category = NotificationCategory.SYSTEM,
            title = "알림",
            content = "내용",
        )

    private fun member(
        id: Long,
        realname: String = "회원$id",
    ): Member =
        Member(
            id = id,
            name = "member$id",
            realname = realname,
            studentStatus = StudentStatus.UNDERGRADUATE,
            status = UserStatus.ACTIVE,
        )
}
