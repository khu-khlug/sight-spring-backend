package com.sight.service

import com.sight.domain.schedule.Schedule
import com.sight.repository.ScheduleRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
) {
    @Transactional(readOnly = true)
    fun listSchedules(
        from: LocalDateTime,
        limit: Int,
    ): List<Schedule> {
        val pageable = PageRequest.of(0, limit)
        return scheduleRepository.findUpcoming(from, pageable)
    }
}
