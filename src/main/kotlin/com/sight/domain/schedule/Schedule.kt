package com.sight.domain.schedule

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "khlug_schedule")
data class Schedule(
    @Id
    val id: Long,
    @Column(name = "category")
    val categoryCode: Long,
    @Column(name = "title", nullable = false, length = 255)
    val title: String,
    @Column(name = "author", nullable = false)
    val author: Long,
    @Column(name = "state", nullable = false, length = 255)
    val state: ScheduleState,
    @Column(name = "scheduled_at", nullable = false)
    val scheduledAt: LocalDateTime,
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun getCategory(): ScheduleCategory? = ScheduleCategory.fromCode(categoryCode)
}
