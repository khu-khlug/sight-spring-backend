package com.sight.service

import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.domain.notification.Notification
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.MemberRepository
import com.sight.repository.NotificationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class NotificationServiceTest {
    private val notificationRepository: NotificationRepository = mock()
    private val memberRepository: MemberRepository = mock()
    private lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        notificationService = NotificationService(notificationRepository, memberRepository)
    }

    @Test
    fun `listNotifications는 사용자의 알림 목록을 페이지네이션하여 반환한다`() {
        // given
        val userId = 1L
        val offset = 0
        val limit = 10
        val notifications =
            listOf(
                Notification(
                    id = "01HQXYZ123456789ABCDEFGHI",
                    userId = userId,
                    category = NotificationCategory.SYSTEM,
                    title = "시스템 알림",
                    content = "시스템 알림 내용입니다.",
                    readAt = null,
                    createdAt = LocalDateTime.now(),
                ),
                Notification(
                    id = "01HQXYZ223456789ABCDEFGHI",
                    userId = userId,
                    category = NotificationCategory.GROUP,
                    title = "그룹 알림",
                    content = "그룹 알림 내용입니다.",
                    readAt = LocalDateTime.now(),
                    createdAt = LocalDateTime.now().minusHours(1),
                ),
            )
        val page = PageImpl(notifications, PageRequest.of(0, limit), 2)

        whenever(notificationRepository.findByUserIdOrderByCreatedAtDesc(any(), any())).thenReturn(page)

        // when
        val result = notificationService.listNotifications(userId, offset, limit)

        // then
        assertEquals(2, result.count)
        assertEquals(2, result.notifications.size)
        assertEquals("시스템 알림", result.notifications[0].title)
        assertEquals("그룹 알림", result.notifications[1].title)
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(any(), any())
    }

    @Test
    fun `readNotifications는 읽지 않은 알림을 읽음 처리한다`() {
        // given
        val userId = 1L
        val notificationIds = listOf("01HQXYZ123456789ABCDEFGHI", "01HQXYZ223456789ABCDEFGHI")
        val unreadNotification =
            Notification(
                id = "01HQXYZ123456789ABCDEFGHI",
                userId = userId,
                category = NotificationCategory.SYSTEM,
                title = "읽지 않은 알림",
                content = "읽지 않은 알림 내용입니다.",
                readAt = null,
                createdAt = LocalDateTime.now(),
            )
        val anotherUnreadNotification =
            Notification(
                id = "01HQXYZ223456789ABCDEFGHI",
                userId = userId,
                category = NotificationCategory.GROUP,
                title = "또 다른 읽지 않은 알림",
                content = "또 다른 읽지 않은 알림 내용입니다.",
                readAt = null,
                createdAt = LocalDateTime.now(),
            )

        whenever(notificationRepository.findByIdInAndUserId(notificationIds, userId))
            .thenReturn(listOf(unreadNotification, anotherUnreadNotification))
        whenever(notificationRepository.saveAll(any<List<Notification>>()))
            .thenAnswer { invocation ->
                invocation.getArgument<List<Notification>>(0)
            }

        // when
        val result = notificationService.readNotifications(userId, notificationIds)

        // then
        assertEquals(2, result.size)
        result.forEach { notification ->
            assertNotNull(notification.readAt)
        }
        verify(notificationRepository).findByIdInAndUserId(notificationIds, userId)
        verify(notificationRepository).saveAll(any<List<Notification>>())
    }

    @Test
    fun `readNotifications는 이미 읽은 알림은 다시 처리하지 않는다`() {
        // given
        val userId = 1L
        val notificationIds = listOf("01HQXYZ123456789ABCDEFGHI", "01HQXYZ223456789ABCDEFGHI")
        val unreadNotification =
            Notification(
                id = "01HQXYZ123456789ABCDEFGHI",
                userId = userId,
                category = NotificationCategory.SYSTEM,
                title = "읽지 않은 알림",
                content = "읽지 않은 알림 내용입니다.",
                readAt = null,
                createdAt = LocalDateTime.now(),
            )
        val alreadyReadNotification =
            Notification(
                id = "01HQXYZ223456789ABCDEFGHI",
                userId = userId,
                category = NotificationCategory.GROUP,
                title = "이미 읽은 알림",
                content = "이미 읽은 알림 내용입니다.",
                readAt = LocalDateTime.now().minusHours(1),
                createdAt = LocalDateTime.now().minusHours(2),
            )

        whenever(notificationRepository.findByIdInAndUserId(notificationIds, userId))
            .thenReturn(listOf(unreadNotification, alreadyReadNotification))
        whenever(notificationRepository.saveAll(any<List<Notification>>()))
            .thenAnswer { invocation ->
                invocation.getArgument<List<Notification>>(0)
            }

        // when
        val result = notificationService.readNotifications(userId, notificationIds)

        // then
        assertEquals(1, result.size)
        assertEquals("01HQXYZ123456789ABCDEFGHI", result[0].id)
        assertNotNull(result[0].readAt)
    }

    @Test
    fun `readNotifications는 다른 사용자의 알림은 처리하지 않는다`() {
        // given
        val userId = 1L
        val notificationIds = listOf("01HQXYZ123456789ABCDEFGHI")

        whenever(notificationRepository.findByIdInAndUserId(notificationIds, userId))
            .thenReturn(emptyList())
        whenever(notificationRepository.saveAll(any<List<Notification>>()))
            .thenAnswer { invocation ->
                invocation.getArgument<List<Notification>>(0)
            }

        // when
        val result = notificationService.readNotifications(userId, notificationIds)

        // then
        assertEquals(0, result.size)
        verify(notificationRepository).findByIdInAndUserId(notificationIds, userId)
    }

    @Test
    fun `createNotification은 새 알림을 생성한다`() {
        // given
        val userId = 1L
        val category = NotificationCategory.SYSTEM
        val title = "새 알림"
        val content = "새 알림 내용입니다."

        whenever(notificationRepository.save(any<Notification>()))
            .thenAnswer { invocation ->
                invocation.getArgument<Notification>(0)
            }

        // when
        val result = notificationService.createNotification(userId, category, title, content)

        // then
        assertNotNull(result.id)
        assertEquals(userId, result.userId)
        assertEquals(category, result.category)
        assertEquals(title, result.title)
        assertEquals(content, result.content)
        assertNull(result.url)
        assertNull(result.readAt)
        verify(notificationRepository).save(any<Notification>())
    }

    @Test
    fun `createNotification은 url이 포함된 알림을 생성한다`() {
        // given
        val userId = 1L
        val category = NotificationCategory.GROUP
        val title = "그룹 알림"
        val content = "그룹에 새로운 활동이 있습니다."
        val url = "/groups/123"

        whenever(notificationRepository.save(any<Notification>()))
            .thenAnswer { invocation ->
                invocation.getArgument<Notification>(0)
            }

        // when
        val result = notificationService.createNotification(userId, category, title, content, url)

        // then
        assertNotNull(result.id)
        assertEquals(userId, result.userId)
        assertEquals(category, result.category)
        assertEquals(title, result.title)
        assertEquals(content, result.content)
        assertEquals(url, result.url)
        assertNull(result.readAt)
        verify(notificationRepository).save(any<Notification>())
    }

    @Test
    fun `createNotificationForManagers는 모든 운영진에게 알림을 생성한다`() {
        // given
        val category = NotificationCategory.SYSTEM
        val title = "운영진 알림"
        val content = "운영진에게 전달할 내용입니다."
        val url = "/admin/dashboard"

        val managers =
            listOf(
                Member(
                    id = 10L,
                    name = "manager1",
                    studentStatus = StudentStatus.UNDERGRADUATE,
                    status = UserStatus.ACTIVE,
                    manager = true,
                ),
                Member(
                    id = 20L,
                    name = "manager2",
                    studentStatus = StudentStatus.UNDERGRADUATE,
                    status = UserStatus.ACTIVE,
                    manager = true,
                ),
            )

        whenever(memberRepository.findByManagerTrue()).thenReturn(managers)
        whenever(notificationRepository.saveAll(any<List<Notification>>()))
            .thenAnswer { invocation ->
                invocation.getArgument<List<Notification>>(0)
            }

        // when
        val result = notificationService.createNotificationForManagers(category, title, content, url)

        // then
        assertEquals(2, result.size)
        assertEquals(10L, result[0].userId)
        assertEquals(20L, result[1].userId)
        result.forEach { notification ->
            assertEquals(category, notification.category)
            assertEquals(title, notification.title)
            assertEquals(content, notification.content)
            assertEquals(url, notification.url)
            assertNotNull(notification.id)
            assertNull(notification.readAt)
        }
        verify(memberRepository).findByManagerTrue()
        verify(notificationRepository).saveAll(any<List<Notification>>())
    }

    @Test
    fun `createNotificationForManagers는 운영진이 없으면 빈 리스트를 반환한다`() {
        // given
        val category = NotificationCategory.SYSTEM
        val title = "운영진 알림"
        val content = "운영진에게 전달할 내용입니다."

        whenever(memberRepository.findByManagerTrue()).thenReturn(emptyList())
        whenever(notificationRepository.saveAll(any<List<Notification>>()))
            .thenAnswer { invocation ->
                invocation.getArgument<List<Notification>>(0)
            }

        // when
        val result = notificationService.createNotificationForManagers(category, title, content)

        // then
        assertEquals(0, result.size)
        verify(memberRepository).findByManagerTrue()
        verify(notificationRepository).saveAll(any<List<Notification>>())
    }
}
