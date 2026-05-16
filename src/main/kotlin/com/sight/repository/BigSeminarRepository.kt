package com.sight.repository

import com.sight.domain.seminar.BigSeminar
import org.springframework.data.jpa.repository.JpaRepository

interface BigSeminarRepository : JpaRepository<BigSeminar, String> {
    fun findFirstByScheduleIdIn(scheduleIds: List<Long>): BigSeminar?
}
