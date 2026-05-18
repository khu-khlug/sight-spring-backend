package com.sight.service

import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.schedule.ScheduleState
import com.sight.repository.ScheduleRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import kotlin.random.Random

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

    @Transactional(readOnly = true)
    fun listInProgressSchedules(limit: Int): List<Schedule> {
        val pageable = PageRequest.of(0, limit)
        return scheduleRepository.findInProgress(LocalDateTime.now(), pageable)
    }

    @Transactional(readOnly = true)
    fun getScheduleById(id: Long): Schedule {
        return scheduleRepository.findActiveById(id)
            ?: throw NotFoundException("존재하지 않는 일정입니다.")
    }

    @Transactional
    fun createSchedule(
        requester: Requester,
        title: String,
        category: ScheduleCategory,
        location: String?,
        scheduledAt: LocalDateTime,
        endAt: LocalDateTime,
        expoint: Int,
        checkCode: String?,
    ): Schedule {
        if (requester.role == UserRole.USER && category != ScheduleCategory.GROUP_ACTIVITY) {
            throw ForbiddenException("USER는 그룹활동 카테고리만 생성 가능합니다.")
        }
        if (!endAt.isAfter(scheduledAt)) {
            throw BadRequestException("종료 시각은 시작 시각 이후여야 합니다.")
        }

        val schedule =
            Schedule(
                id = pickAvailableScheduleId(),
                category = category,
                title = title,
                author = requester.userId,
                state = ScheduleState.PUBLIC,
                scheduledAt = scheduledAt,
                endAt = endAt,
                location = location,
                expoint = expoint,
                checkCode = checkCode ?: generateCheckCode(),
            )
        return scheduleRepository.save(schedule)
    }

    @Transactional
    fun updateSchedule(
        requester: Requester,
        id: Long,
        title: String,
        category: ScheduleCategory,
        location: String?,
        scheduledAt: LocalDateTime,
        endAt: LocalDateTime,
        expoint: Int,
    ): Schedule {
        val existing =
            scheduleRepository.findActiveById(id)
                ?: throw NotFoundException("존재하지 않는 일정입니다.")

        if (requester.role == UserRole.USER) {
            if (existing.author != requester.userId) {
                throw ForbiddenException("본인이 작성한 일정만 수정 가능합니다.")
            }
            if (existing.category != ScheduleCategory.GROUP_ACTIVITY) {
                throw ForbiddenException("그룹활동 카테고리 일정만 수정 가능합니다.")
            }
            if (category != ScheduleCategory.GROUP_ACTIVITY) {
                throw ForbiddenException("category를 그룹활동 외로 변경할 수 없습니다.")
            }
        }

        if (!endAt.isAfter(scheduledAt)) {
            throw BadRequestException("종료 시각은 시작 시각 이후여야 합니다.")
        }

        val updated =
            existing.copy(
                title = title,
                category = category,
                location = location,
                scheduledAt = scheduledAt,
                endAt = endAt,
                expoint = expoint,
                updatedAt = LocalDateTime.now(),
            )
        return scheduleRepository.save(updated)
    }

    @Transactional
    fun deleteSchedule(
        requester: Requester,
        id: Long,
    ) {
        val existing =
            scheduleRepository.findActiveById(id)
                ?: throw NotFoundException("존재하지 않는 일정입니다.")

        if (requester.role == UserRole.USER) {
            if (existing.author != requester.userId) {
                throw ForbiddenException("본인이 작성한 일정만 삭제 가능합니다.")
            }
            if (existing.category != ScheduleCategory.GROUP_ACTIVITY) {
                throw ForbiddenException("그룹활동 카테고리 일정만 삭제 가능합니다.")
            }
        }

        val trashed =
            existing.copy(
                state = ScheduleState.TRASH,
                updatedAt = LocalDateTime.now(),
            )
        scheduleRepository.save(trashed)
    }

    private fun pickAvailableScheduleId(): Long {
        repeat(MAX_SCHEDULE_ID_RETRY + 1) {
            val id = createNewScheduleId()
            if (!scheduleRepository.existsById(id)) return id
        }
        throw IllegalStateException("schedule ID 채번 $MAX_SCHEDULE_ID_RETRY 회 retry 후에도 충돌")
    }

    private fun createNewScheduleId(): Long {
        val minimumId = 1_000_000
        val millisUntil20250101 =
            LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0, 0)
                .atZone(KST).toInstant().toEpochMilli()
        val currentTimestamp = System.currentTimeMillis()
        val timePart = (currentTimestamp - millisUntil20250101) / 1000 / 60
        val randomPart = Random.nextLong(0L, 1000L)
        return minimumId + timePart * 1000 + randomPart
    }

    private fun generateCheckCode(): String {
        return "%04d".format(Random.nextInt(0, 10000))
    }

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
        private const val MAX_SCHEDULE_ID_RETRY = 3
    }
}
