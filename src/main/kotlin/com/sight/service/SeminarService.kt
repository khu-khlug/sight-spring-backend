package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.schedule.ScheduleState
import com.sight.repository.BigSeminarRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

internal val SEMINAR_TIME_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

data class OpenSeminarResult(
    val id: String,
    val isSummerSeason: Boolean,
    val isSpeakAfter: Boolean,
    val scheduledAt: Instant,
    val endAt: Instant,
)

@Service
class SeminarService(private val bigSeminarRepository: BigSeminarRepository) {
    @Transactional(readOnly = true)
    fun getOpenSeminar(at: Instant): OpenSeminarResult? =
        bigSeminarRepository.findOpenSeminars(ScheduleState.PUBLIC, LocalDateTime.ofInstant(at, SEMINAR_TIME_ZONE), PageRequest.of(0, 1))
            .firstOrNull()?.let {
                OpenSeminarResult(
                    it.id,
                    it.summerSeason,
                    it.speakAfter,
                    it.scheduledAt.atZone(SEMINAR_TIME_ZONE).toInstant(),
                    it.endAt.atZone(SEMINAR_TIME_ZONE).toInstant(),
                )
            }

    @Transactional(readOnly = true)
    fun requireActivityReportPeriod(seminarId: String) {
        val endAt =
            bigSeminarRepository.findScheduleEndAt(seminarId, ScheduleState.PUBLIC)
                ?: throw NotFoundException("세미나 일정을 찾을 수 없습니다.")
        if (endAt <= LocalDateTime.now(SEMINAR_TIME_ZONE)) throw BadRequestException("접수 기간이 지났습니다.")
    }
}
