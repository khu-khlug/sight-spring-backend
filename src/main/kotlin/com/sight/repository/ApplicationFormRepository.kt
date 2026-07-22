package com.sight.repository

import com.sight.domain.application.ApplicationForm
import com.sight.domain.application.ApplicationFormStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ApplicationFormRepository : JpaRepository<ApplicationForm, String> {
    @Query(
        "select distinct form from ApplicationForm form join InterviewAvailableTime time on time.applicationFormId = form.id " +
            "where (:date is null or form.createdAt >= :date) and time.availableAt in :interviewTimes",
    )
    fun findAllByInterviewTimes(
        @Param("date") date: LocalDateTime?,
        @Param("interviewTimes") interviewTimes: List<String>,
        pageable: Pageable,
    ): Page<ApplicationForm>

    fun findAllByCreatedAtGreaterThanEqual(
        createdAt: LocalDateTime,
        pageable: Pageable,
    ): Page<ApplicationForm>

    fun findFirstByInfo21IdAndStatusInOrderByUpdatedAtDesc(
        info21Id: String,
        statuses: Collection<ApplicationFormStatus>,
    ): ApplicationForm?
}
