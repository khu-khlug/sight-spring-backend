package com.sight.repository

import com.sight.domain.application.ApplicationQuestion
import org.springframework.data.jpa.repository.JpaRepository

interface ApplicationQuestionRepository : JpaRepository<ApplicationQuestion, String> {
    fun findAllByIsExposedTrue(): List<ApplicationQuestion>
}
