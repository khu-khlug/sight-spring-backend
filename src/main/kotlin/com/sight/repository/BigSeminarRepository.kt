package com.sight.repository

import com.sight.domain.schedule.ScheduleState
import com.sight.domain.seminar.BigSeminar
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface BigSeminarRepository : JpaRepository<BigSeminar, String> {
    @Query(
        """
        select b.id as id, b.isSummerSeason as summerSeason, b.isSpeakAfter as speakAfter,
               s.scheduledAt as scheduledAt, s.endAt as endAt
        from BigSeminar b join Schedule s on s.id = b.scheduleId
        where s.state = :state and s.endAt > :now
        order by s.scheduledAt, s.id, b.id
        """,
    )
    fun findOpenSeminars(
        @Param("state") state: ScheduleState,
        @Param("now") now: LocalDateTime,
        pageable: Pageable,
    ): List<OpenSeminarView>

    @Query(
        "select s.endAt from BigSeminar b join Schedule s on s.id = b.scheduleId where b.id = :seminarId and s.state = :state",
    )
    fun findScheduleEndAt(
        @Param("seminarId") seminarId: String,
        @Param("state") state: ScheduleState,
    ): LocalDateTime?

    fun findByScheduleId(scheduleId: Long): BigSeminar?

    fun deleteByScheduleId(scheduleId: Long)
}

interface OpenSeminarView {
    val id: String
    val summerSeason: Boolean
    val speakAfter: Boolean
    val scheduledAt: LocalDateTime
    val endAt: LocalDateTime
}
