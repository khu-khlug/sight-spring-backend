package com.sight.repository

import com.sight.domain.schedule.Schedule
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ScheduleRepository : JpaRepository<Schedule, Long> {
    @Query("SELECT s FROM Schedule s WHERE s.scheduledAt >= :from ORDER BY s.scheduledAt ASC")
    fun findUpcoming(
        @Param("from") from: LocalDateTime,
        pageable: Pageable,
    ): List<Schedule>
}
