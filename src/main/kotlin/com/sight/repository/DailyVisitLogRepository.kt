package com.sight.repository

import com.sight.domain.room.DailyVisitLog
import com.sight.domain.room.DailyVisitLogId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface DailyVisitLogRepository : JpaRepository<DailyVisitLog, DailyVisitLogId> {
    fun findByRoomNumberAndMemberId(
        roomNumber: Int,
        memberId: Long,
    ): DailyVisitLog?

    fun countByDateAndRoomNumber(
        date: LocalDate,
        roomNumber: Int,
    ): Long
}
