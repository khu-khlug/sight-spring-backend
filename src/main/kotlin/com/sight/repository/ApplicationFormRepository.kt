package com.sight.repository

import com.sight.domain.application.ApplicationForm
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ApplicationFormRepository : JpaRepository<ApplicationForm, String>
