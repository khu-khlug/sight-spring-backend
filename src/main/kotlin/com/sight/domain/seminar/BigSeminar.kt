package com.sight.domain.seminar

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "big_seminar")
data class BigSeminar(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "schedule_id", nullable = false)
    val scheduleId: Long,

    @Column(name = "is_summer_season", nullable = false, columnDefinition = "TINYINT")
    val isSummerSeason: Boolean,

    @Column(name = "is_speak_after", nullable = false, columnDefinition = "TINYINT")
    val isSpeakAfter: Boolean,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
