package com.sight.repository

import com.sight.domain.schedule.Schedule
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ScheduleRepository : JpaRepository<Schedule, Long> {
    fun findByEndAtAfterOrderByScheduledAtAsc(endAt: LocalDateTime): List<Schedule>

    @Query("SELECT s FROM Schedule s WHERE s.scheduledAt >= :from AND s.state = 'public' ORDER BY s.scheduledAt ASC")
    fun findUpcoming(
        @Param("from") from: LocalDateTime,
        pageable: Pageable,
    ): List<Schedule>

    @Query("SELECT s FROM Schedule s WHERE s.state = 'public' ORDER BY s.scheduledAt ASC")
    fun findAllActive(pageable: Pageable): List<Schedule>

    @Query("SELECT s FROM Schedule s WHERE s.id = :id AND s.state = 'public'")
    fun findActiveById(
        @Param("id") id: Long,
    ): Schedule?

    @Modifying
    @Query("UPDATE Schedule s SET s.state = 'trash', s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :id AND s.state = 'public'")
    fun deleteActiveById(
        @Param("id") id: Long,
    )

    @Query(
        "SELECT COUNT(s) FROM Schedule s " +
            "WHERE s.location = :location AND s.state = 'public' " +
            "AND s.scheduledAt < :endAt AND s.endAt > :scheduledAt",
    )
    fun countOverlappingAtLocation(
        @Param("location") location: String,
        @Param("scheduledAt") scheduledAt: LocalDateTime,
        @Param("endAt") endAt: LocalDateTime,
    ): Long
}
