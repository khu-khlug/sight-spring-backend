package com.sight.domain.schedule

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.io.Serializable
import java.time.LocalDateTime

data class ScheduleMemberApplyId(
    val memberId: Long = 0,
    val scheduleId: Long = 0,
) : Serializable

@Entity
@Table(name = "khlug_schedule_member_apply")
@IdClass(ScheduleMemberApplyId::class)
data class ScheduleMemberApply(
    @Id
    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Id
    @Column(name = "schedule_id", nullable = false)
    val scheduleId: Long,

    @Column(name = "attended_at")
    val attendedAt: LocalDateTime? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
