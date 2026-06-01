package com.sight.repository

import com.sight.domain.application.ApplicationForm
import com.sight.domain.application.ApplicationFormStatus
import org.springframework.data.jpa.repository.JpaRepository

interface ApplicationFormRepository : JpaRepository<ApplicationForm, String> {
    fun findFirstByInfo21IdAndStatusInOrderByUpdatedAtDesc(
        info21Id: String,
        statuses: Collection<ApplicationFormStatus>,
    ): ApplicationForm?
}
