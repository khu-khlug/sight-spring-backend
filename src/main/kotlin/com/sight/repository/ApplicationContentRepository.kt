package com.sight.repository

import com.sight.domain.application.ApplicationContent
import org.springframework.data.jpa.repository.JpaRepository

interface ApplicationContentRepository : JpaRepository<ApplicationContent, String> {
    fun findAllByApplicationFormId(applicationFormId: String): List<ApplicationContent>
}
