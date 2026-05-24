package com.sight.service

import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ConflictException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnauthorizedException
import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.schedule.ScheduleMemberApply
import com.sight.domain.schedule.ScheduleState
import com.sight.repository.ScheduleMemberApplyRepository
import com.sight.repository.ScheduleRepository
import com.sight.service.dto.CheckScheduleAttendanceResult
import com.sight.service.dto.ListScheduleAttendancesResult
import com.sight.service.dto.ScheduleAttendanceItem
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
    private val scheduleMemberApplyRepository: ScheduleMemberApplyRepository,
    private val pointService: PointService,
) {
    @Transactional(readOnly = true)
    fun listSchedules(
        from: LocalDateTime?,
        limit: Int,
    ): List<Schedule> {
        val pageable = PageRequest.of(0, limit)
        return if (from != null) {
            scheduleRepository.findUpcoming(from, pageable)
        } else {
            scheduleRepository.findAllActive(pageable)
        }
    }

    @Transactional(readOnly = true)
    fun listActiveSchedules(): List<Schedule> {
        val now = LocalDateTime.now()
        val pageable = PageRequest.of(0, DEFAULT_ACTIVE_SCHEDULE_LIMIT)
        return scheduleRepository.findAttendanceActive(now, pageable)
            .filter { it.isAttendanceActive(now) }
    }

    @Transactional(readOnly = true)
    fun getScheduleById(id: Long): Schedule {
        return scheduleRepository.findActiveById(id)
            ?: throw NotFoundException("존재하지 않는 일정입니다.")
    }

    @Transactional(readOnly = true)
    fun listScheduleAttendances(scheduleId: Long): ListScheduleAttendancesResult {
        if (scheduleRepository.findActiveById(scheduleId) == null) {
            throw NotFoundException("존재하지 않는 일정입니다.")
        }

        val attendances =
            scheduleMemberApplyRepository.findByScheduleIdOrderByCreatedAtAsc(scheduleId).map { apply ->
                ScheduleAttendanceItem(
                    userId = apply.memberId,
                    isChecked = apply.attendedAt != null,
                    createdAt = apply.createdAt,
                )
            }

        return ListScheduleAttendancesResult(attendances = attendances)
    }

    @Transactional
    fun checkScheduleAttendance(
        requester: Requester,
        scheduleId: Long,
        code: String,
    ): CheckScheduleAttendanceResult {
        val schedule =
            scheduleRepository.findActiveById(scheduleId)
                ?: throw NotFoundException("존재하지 않는 일정입니다.")

        if (scheduleMemberApplyRepository.existsByMemberIdAndScheduleId(requester.userId, scheduleId)) {
            throw ConflictException("이미 출석체크한 일정입니다.")
        }

        val now = LocalDateTime.now()
        if (schedule.checkCode == null) {
            throw BadRequestException("출석 코드가 설정되지 않은 일정입니다.")
        }
        if (now.isBefore(schedule.scheduledAt) || now.isAfter(schedule.endAt)) {
            throw BadRequestException("출석체크 가능한 시간이 아닙니다.")
        }
        if (code != schedule.checkCode) {
            throw UnauthorizedException("출석 코드가 일치하지 않습니다.")
        }

        val attendance =
            scheduleMemberApplyRepository.save(
                ScheduleMemberApply(
                    memberId = requester.userId,
                    scheduleId = scheduleId,
                    attendedAt = now,
                ),
            )

        if (schedule.expoint > 0) {
            pointService.givePoint(
                targetUserId = requester.userId,
                point = schedule.expoint,
                message = "${schedule.title} 출석",
            )
        }

        return CheckScheduleAttendanceResult(
            scheduleId = attendance.scheduleId,
            userId = attendance.memberId,
            expointGranted = schedule.expoint,
            createdAt = attendance.createdAt,
        )
    }

    @Transactional
    fun removeScheduleAttendance(
        requester: Requester,
        scheduleId: Long,
        userId: Long,
    ) {
        if (requester.role != UserRole.MANAGER) {
            throw ForbiddenException("권한이 부족합니다.")
        }

        val schedule =
            scheduleRepository.findActiveById(scheduleId)
                ?: throw NotFoundException("존재하지 않는 일정입니다.")
        val attendance =
            scheduleMemberApplyRepository.findByMemberIdAndScheduleId(
                memberId = userId,
                scheduleId = scheduleId,
            ) ?: throw NotFoundException("출석 기록을 찾을 수 없습니다.")

        scheduleMemberApplyRepository.delete(attendance)

        if (schedule.expoint > 0) {
            pointService.givePoint(
                targetUserId = userId,
                point = -schedule.expoint,
                message = "${schedule.title} 출석 취소",
            )
        }
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
                checkCode = checkCode,
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

    private fun Schedule.isAttendanceActive(now: LocalDateTime): Boolean {
        return !scheduledAt.isAfter(now) && !endAt.isBefore(now) && checkCode != null
    }

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
        private const val DEFAULT_ACTIVE_SCHEDULE_LIMIT = 50
        private const val MAX_SCHEDULE_ID_RETRY = 3
    }
}
