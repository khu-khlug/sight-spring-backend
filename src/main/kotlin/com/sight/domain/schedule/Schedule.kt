package com.sight.domain.schedule

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "schedule")
data class Schedule(
    @Id
    val id: Long,

    @Column(name = "category", nullable = false, length = 255)
    @Enumerated(EnumType.STRING)
    val category: ScheduleCategory,

    @Column(name = "title", nullable = false, length = 255)
    val title: String,

    @Column(name = "author", nullable = false)
    val author: Long,

    @Column(name = "state", nullable = false, length = 255)
    @Convert(converter = ScheduleStateConverter::class)
    val state: ScheduleState,

    @Column(name = "scheduled_at", nullable = false)
    val scheduledAt: LocalDateTime,

    @Column(name = "end_at", nullable = false)
    val endAt: LocalDateTime,

    @Column(name = "location", length = 255)
    val location: String? = null,

    @Column(name = "expoint", nullable = false)
    val expoint: Int = 0,

    @Column(name = "check_code", length = 255)
    val checkCode: String? = null,

    @Column(name = "group_id")
    val groupId: Long? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
