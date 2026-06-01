package com.sight.repository

import com.sight.domain.application.UserRegistrationRequest
import org.springframework.data.jpa.repository.JpaRepository

interface UserRegistrationRequestRepository : JpaRepository<UserRegistrationRequest, String>
