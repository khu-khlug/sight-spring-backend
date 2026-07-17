package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.ConflictException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnauthorizedException
import com.sight.core.info21.Info21AuthClient
import com.sight.core.info21.Info21AuthRequest
import com.sight.domain.application.UserRegistrationRequest
import com.sight.domain.application.UserRegistrationRequestStatus
import com.sight.domain.member.UserStatus
import com.sight.repository.MemberRepository
import com.sight.repository.UserRegistrationRequestRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserRegistrationRequestService(
    private val memberRepository: MemberRepository,
    private val userRegistrationRequestRepository: UserRegistrationRequestRepository,
    private val info21AuthClient: Info21AuthClient,
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

    @Transactional
    fun createRegistrationRequest(
        info21Id: String,
        info21Password: String,
        requestedUserId: Long,
    ): UserRegistrationRequest {
        // 1. 해당 info21Id로 이미 가입된 회원이 존재하는지 확인합니다. 존재하면 409.
        val existingMember = memberRepository.findByName(info21Id)
        if (existingMember != null && existingMember.status == UserStatus.ACTIVE) {
            throw ConflictException("이미 가입 완료된 회원입니다: $info21Id")
        }

        // 2. 해당 info21Id로 이미 대기중인 상태의 UserRegistrationRequest가 존재하는지 확인합니다. 존재하면 409.
        if (existingMember != null) {
            val hasPendingRequest =
                userRegistrationRequestRepository.existsByRequestedUserIdAndStatus(
                    requestedUserId = existingMember.id,
                    status = UserRegistrationRequestStatus.PENDING,
                )
            if (hasPendingRequest) {
                throw ConflictException("이미 대기중인 회원 등록 요청이 존재합니다: $info21Id")
            }
        }

        // 3. info21Id와 info21Password로 info21 인증 요청하여 성공하는지 확인합니다. 실패 시 401.
        val authResponse =
            info21AuthClient.authenticate(
                Info21AuthRequest(
                    info21Id = info21Id,
                    info21Password = info21Password,
                ),
            )
        if (authResponse.code != 200) {
            throw UnauthorizedException("info21 인증에 실패하였습니다")
        }

        // 4. UserRegistrationRequest 객체를 생성합니다. (id = ulid, requestedUserId = 요청한 유저 ID, status = 대기중)
        val registrationRequest =
            UserRegistrationRequest(
                id = UlidCreator.getUlid().toString(),
                requestedUserId = requestedUserId,
                status = UserRegistrationRequestStatus.PENDING,
            )

        // 5. 저장합니다.
        return userRegistrationRequestRepository.save(registrationRequest)
    }
}
