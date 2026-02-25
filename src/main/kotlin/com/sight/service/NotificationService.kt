package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.domain.notification.Notification
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.MemberRepository
import com.sight.repository.NotificationRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class NotificationListResult(
    val count: Long,
    val notifications: List<Notification>,
)

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val memberRepository: MemberRepository,
) {
    @Transactional(readOnly = true)
    fun listNotifications(
        userId: Long,
        offset: Int,
        limit: Int,
    ): NotificationListResult {
        val pageNumber = offset / limit
        val pageable = PageRequest.of(pageNumber, limit)
        val page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)

        return NotificationListResult(
            count = page.totalElements,
            notifications = page.content,
        )
    }

    @Transactional
    fun readNotifications(
        userId: Long,
        notificationIds: List<String>,
    ): List<Notification> {
        val notifications = notificationRepository.findByIdInAndUserId(notificationIds, userId)
        val now = LocalDateTime.now()

        val updatedNotifications =
            notifications.mapNotNull { notification ->
                if (notification.readAt == null) {
                    notification.copy(readAt = now)
                } else {
                    null
                }
            }

        return notificationRepository.saveAll(updatedNotifications)
    }

    @Transactional
    fun createNotification(
        userId: Long,
        category: NotificationCategory,
        title: String,
        content: String,
        url: String? = null,
    ): Notification {
        val notification =
            Notification(
                id = UlidCreator.getUlid().toString(),
                userId = userId,
                category = category,
                title = title,
                content = content,
                url = url,
            )

        return notificationRepository.save(notification)
    }

    @Transactional
    fun createNotificationForManagers(
        category: NotificationCategory,
        title: String,
        content: String,
        url: String? = null,
    ): List<Notification> {
        val managers = memberRepository.findByManagerTrue()

        val notifications =
            managers.map { manager ->
                Notification(
                    id = UlidCreator.getUlid().toString(),
                    userId = manager.id,
                    category = category,
                    title = title,
                    content = content,
                    url = url,
                )
            }

        return notificationRepository.saveAll(notifications)
    }
}
