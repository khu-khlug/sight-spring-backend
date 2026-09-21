package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.room.CLUB_ROOM_LOCATIONS
import com.sight.domain.room.DailyVisitLog
import com.sight.repository.DailyVisitLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Service
class RoomService(
    private val dailyVisitLogRepository: DailyVisitLogRepository,
) {
    @Transactional(readOnly = true)
    fun getDailyVisitCount(roomNumber: Int): Long {
        if (roomNumber !in CLUB_ROOM_LOCATIONS) {
            throw BadRequestException("유효하지 않은 방 번호입니다")
        }
        val today = LocalDate.now(KST)
        return dailyVisitLogRepository.countByDateAndRoomNumber(today, roomNumber)
    }

    @Transactional
    fun upsertDailyVisitLog(
        roomNumber: Int,
        memberId: Long,
        date: LocalDate,
    ) {
        val existing = dailyVisitLogRepository.findByRoomNumberAndMemberId(roomNumber, memberId)
        if (existing != null) {
            existing.visit(date)
            dailyVisitLogRepository.save(existing)
        } else {
            dailyVisitLogRepository.save(DailyVisitLog(roomNumber = roomNumber, memberId = memberId, date = date))
        }
    }

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
