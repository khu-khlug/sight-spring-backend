package com.sight.repository

import com.sight.domain.seminar.BigSeminar
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BigSeminarRepository : JpaRepository<BigSeminar, String> {
    fun findByScheduleId(scheduleId: Long): BigSeminar?

    fun deleteByScheduleId(scheduleId: Long)
}
