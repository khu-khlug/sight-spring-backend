package com.sight.repository

import com.sight.domain.application.InterviewAvailableTime
import org.springframework.data.jpa.repository.JpaRepository

interface InterviewAvailableTimeRepository : JpaRepository<InterviewAvailableTime, String> {
    fun findAllByApplicationFormId(applicationFormId: String): List<InterviewAvailableTime>
}
