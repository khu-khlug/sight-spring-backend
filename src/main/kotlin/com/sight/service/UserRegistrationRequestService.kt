package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.domain.application.UserRegistrationRequest
import com.sight.domain.application.UserRegistrationRequestStatus
import com.sight.repository.UserRegistrationRequestRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserRegistrationRequestService(
    private val userRegistrationRequestRepository: UserRegistrationRequestRepository,
) {
    @Transactional
    fun approve(requestId: String): UserRegistrationRequest {
        val userRegistrationRequest =
            userRegistrationRequestRepository.findById(requestId).orElseThrow {
                NotFoundException("회원 등록 요청을 찾을 수 없습니다")
            }

        return userRegistrationRequestRepository.save(
            userRegistrationRequest.copy(
                status = UserRegistrationRequestStatus.APPROVED,
                updatedAt = LocalDateTime.now(),
            ),
        )
    }
}
