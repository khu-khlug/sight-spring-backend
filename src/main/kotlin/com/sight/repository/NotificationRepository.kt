package com.sight.repository

import com.sight.domain.notification.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface NotificationRepository : JpaRepository<Notification, String> {
    @Query(
        """
        SELECT n FROM Notification n
        WHERE n.userId = :userId
        ORDER BY n.createdAt DESC
        """,
    )
    fun findByUserIdOrderByCreatedAtDesc(
        userId: Long,
        pageable: Pageable,
    ): Page<Notification>

    fun findByIdInAndUserId(
        ids: List<String>,
        userId: Long,
    ): List<Notification>
}
