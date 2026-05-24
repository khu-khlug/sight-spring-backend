package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ConflictException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnauthorizedException
import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.schedule.ScheduleMemberApply
import com.sight.domain.schedule.ScheduleState
import com.sight.domain.seminar.BigSeminar
import com.sight.repository.BigSeminarRepository
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
    private val bigSeminarRepository: BigSeminarRepository,
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
    fun createSchedule(
        requesterUserId: Long,
        title: String,
        category: ScheduleCategory,
        location: String?,
        scheduledAt: LocalDateTime,
        endAt: LocalDateTime,
        expoint: Int,
        generateCheckCode: Boolean,
    ): Schedule {
        if (!category.isManagerCategory) {
            throw BadRequestException("그룹 활동·세미나 일정은 전용 엔드포인트를 사용해 주세요.")
        }
        return saveNewSchedule(requesterUserId, title, category, location, scheduledAt, endAt, expoint, generateCheckCode)
    }

    @Transactional
    fun updateSchedule(
        id: Long,
        title: String,
        location: String?,
        scheduledAt: LocalDateTime,
        endAt: LocalDateTime,
        expoint: Int,
    ): Schedule {
        val existing = findActiveScheduleInTier(id) { it.isManagerCategory }
        return applyScheduleUpdate(existing, title, location, scheduledAt, endAt, expoint)
    }

    @Transactional
    fun updateScheduleCategory(
        id: Long,
        category: ScheduleCategory,
        isSummerSeason: Boolean?,
        isSpeakAfter: Boolean?,
    ): Pair<Schedule, BigSeminar?> {
        if (category.isGroupActivity) {
            throw BadRequestException("그룹 활동 카테고리로는 변경할 수 없습니다.")
        }
        val existing =
            scheduleRepository.findActiveById(id)
                ?: throw NotFoundException("존재하지 않는 일정입니다.")

        val updated = scheduleRepository.save(existing.copy(category = category, updatedAt = LocalDateTime.now()))

        val bigSeminar =
            when {
                category.isSeminar -> {
                    val summer = isSummerSeason ?: throw BadRequestException("세미나로 변경하려면 isSummerSeason이 필요합니다.")
                    val speakAfter = isSpeakAfter ?: throw BadRequestException("세미나로 변경하려면 isSpeakAfter가 필요합니다.")
                    upsertBigSeminar(updated.id, summer, speakAfter)
                }
                existing.category.isSeminar -> {
                    bigSeminarRepository.deleteByScheduleId(updated.id)
                    null
                }
                else -> null
            }
        return updated to bigSeminar
    }

    @Transactional
    fun deleteSchedule(id: Long) {
        val existing = findActiveScheduleInTier(id) { it.isManagerCategory }
        scheduleRepository.deleteActiveById(existing.id)
    }

    // ===== 공통 helper =====

    private fun saveNewSchedule(
        requesterUserId: Long,
        title: String,
        category: ScheduleCategory,
        location: String?,
        scheduledAt: LocalDateTime,
        endAt: LocalDateTime,
        expoint: Int,
        generateCheckCode: Boolean,
    ): Schedule {
        validateTimeRange(scheduledAt, endAt)
        val schedule =
            Schedule(
                id = pickAvailableScheduleId(),
                category = category,
                title = title,
                author = requesterUserId,
                state = ScheduleState.PUBLIC,
                scheduledAt = scheduledAt,
                endAt = endAt,
                location = location,
                expoint = expoint,
                checkCode = if (generateCheckCode) createCheckCode() else null,
            )
        return scheduleRepository.save(schedule)
    }

    private fun applyScheduleUpdate(
        existing: Schedule,
        title: String,
        location: String?,
        scheduledAt: LocalDateTime,
        endAt: LocalDateTime,
        expoint: Int,
    ): Schedule {
        validateTimeRange(scheduledAt, endAt)
        return scheduleRepository.save(
            existing.copy(
                title = title,
                location = location,
                scheduledAt = scheduledAt,
                endAt = endAt,
                expoint = expoint,
                updatedAt = LocalDateTime.now(),
            ),
        )
    }

    private fun upsertBigSeminar(
        scheduleId: Long,
        isSummerSeason: Boolean,
        isSpeakAfter: Boolean,
    ): BigSeminar {
        val existing = bigSeminarRepository.findByScheduleId(scheduleId)
        val entity =
            existing?.copy(isSummerSeason = isSummerSeason, isSpeakAfter = isSpeakAfter)
                ?: BigSeminar(
                    id = scheduleId.toString(),
                    scheduleId = scheduleId,
                    isSummerSeason = isSummerSeason,
                    isSpeakAfter = isSpeakAfter,
                )
        return bigSeminarRepository.save(entity)
    }

    private fun findActiveScheduleInTier(
        id: Long,
        inTier: (ScheduleCategory) -> Boolean,
    ): Schedule {
        val existing =
            scheduleRepository.findActiveById(id)
                ?: throw NotFoundException("존재하지 않는 일정입니다.")
        if (!inTier(existing.category)) {
            throw BadRequestException("이 엔드포인트로 처리할 수 없는 카테고리의 일정입니다.")
        }
        return existing
    }

    private fun validateTimeRange(
        scheduledAt: LocalDateTime,
        endAt: LocalDateTime,
    ) {
        if (!endAt.isAfter(scheduledAt)) {
            throw BadRequestException("종료 시각은 시작 시각 이후여야 합니다.")
        }
    }

    private fun createCheckCode(): String = "%04d".format(Random.nextInt(10000))

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

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
        private const val MAX_SCHEDULE_ID_RETRY = 3
    }
}
