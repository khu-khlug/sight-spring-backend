package com.sight.repository

import com.sight.domain.application.UserRegistrationRequest
import com.sight.domain.application.UserRegistrationRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRegistrationRequestRepository : JpaRepository<UserRegistrationRequest, String> {
    fun existsByRequestedUserIdAndStatus(
        requestedUserId: Long,
        status: UserRegistrationRequestStatus,
    ): Boolean
}
