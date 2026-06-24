package com.sight.repository

import com.sight.domain.application.ApplicationFormAuthToken
import org.springframework.data.jpa.repository.JpaRepository

interface ApplicationFormAuthTokenRepository : JpaRepository<ApplicationFormAuthToken, String>
