package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnauthorizedException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.core.info21.Info21AuthClient
import com.sight.core.info21.Info21AuthRequest
import com.sight.domain.application.ApplicationContent
import com.sight.domain.application.ApplicationForm
import com.sight.domain.application.ApplicationFormAuthToken
import com.sight.domain.application.ApplicationFormStatus
import com.sight.domain.application.ApplicationQuestion
import com.sight.repository.ApplicationContentRepository
import com.sight.repository.ApplicationFormAuthTokenRepository
import com.sight.repository.ApplicationFormRepository
import com.sight.repository.ApplicationQuestionRepository
import com.sight.repository.InterviewAvailableTimeRepository
import com.sight.repository.MemberRepository
import com.sight.service.dto.ApplicationFormDraftDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime

@Service
class ApplicationFormService(
    private val info21AuthClient: Info21AuthClient,
    private val applicationFormRepository: ApplicationFormRepository,
    private val applicationQuestionRepository: ApplicationQuestionRepository,
    private val applicationContentRepository: ApplicationContentRepository,
    private val applicationFormAuthTokenRepository: ApplicationFormAuthTokenRepository,
    private val interviewAvailableTimeRepository: InterviewAvailableTimeRepository,
    private val memberRepository: MemberRepository,
) {
    private val reusableStatuses = listOf(ApplicationFormStatus.DRAFT, ApplicationFormStatus.SUBMITTED)
    private val tokenCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private val secureRandom = SecureRandom()

    @Transactional
    fun createDraft(
        info21Id: String,
        info21Password: String,
    ): ApplicationFormDraftDto {
        val authResponse =
            info21AuthClient.authenticate(
                Info21AuthRequest(
                    info21Id = info21Id,
                    info21Password = info21Password,
                ),
            )
        if (authResponse.code != 200) {
            throw UnauthorizedException("Info21 인증에 실패했습니다")
        }

        val applicationForm =
            applicationFormRepository.findFirstByInfo21IdAndStatusInOrderByUpdatedAtDesc(
                info21Id = info21Id,
                statuses = reusableStatuses,
            ) ?: createApplicationForm(info21Id, authResponse.data.name)

        val token = saveAuthToken(applicationForm.id)
        val contents = applicationContentRepository.findAllByApplicationFormId(applicationForm.id)
        val interviewAvailableTimes =
            interviewAvailableTimeRepository.findAllByApplicationFormId(applicationForm.id)

        return ApplicationFormDraftDto(
            id = applicationForm.id,
            info21Id = applicationForm.info21Id,
            submittee = applicationForm.submittee,
            token = token.token,
            status = applicationForm.status,
            interviewAvailableTimes =
                interviewAvailableTimes.map { availableTime ->
                    ApplicationFormDraftDto.InterviewAvailableTimeDto(
                        id = availableTime.id,
                        availableAt = availableTime.availableAt,
                        createdAt = availableTime.createdAt,
                    )
                },
            contents =
                contents.map { content ->
                    ApplicationFormDraftDto.ApplicationContentDto(
                        id = content.id,
                        questionId = content.questionId,
                        content = content.content,
                        createdAt = content.createdAt,
                        updatedAt = content.updatedAt,
                    )
                },
            createdAt = applicationForm.createdAt,
            updatedAt = applicationForm.updatedAt,
        )
    }

    @Transactional
    fun assignManager(
        applicationFormId: String,
        managerUserId: Long,
    ) {
        val manager =
            memberRepository.findById(managerUserId)
                .orElseThrow { UnprocessableEntityException("담당자로 배정할 운영진을 찾을 수 없습니다") }
        if (!manager.manager) {
            throw UnprocessableEntityException("담당자로 배정할 사용자가 운영진이 아닙니다")
        }

        val applicationForm =
            applicationFormRepository.findById(applicationFormId)
                .orElseThrow { NotFoundException("가입신청서를 찾을 수 없습니다") }

        applicationFormRepository.save(applicationForm.copy(assignedUserId = managerUserId))
    }

    private fun createApplicationForm(
        info21Id: String,
        submittee: String,
    ): ApplicationForm {
        val applicationForm =
            ApplicationForm(
                id = UlidCreator.getUlid().toString(),
                info21Id = info21Id,
                submittee = submittee,
                status = ApplicationFormStatus.DRAFT,
            )
        applicationFormRepository.save(applicationForm)

        val contents =
            applicationQuestionRepository.findAllByIsExposedTrue()
                .sortedWith(compareBy<ApplicationQuestion> { it.order ?: Int.MAX_VALUE }.thenBy { it.createdAt })
                .map { question ->
                    ApplicationContent(
                        id = UlidCreator.getUlid().toString(),
                        applicationFormId = applicationForm.id,
                        questionId = question.id,
                        content = "",
                    )
                }
        applicationContentRepository.saveAll(contents)

        return applicationForm
    }

    private fun saveAuthToken(applicationFormId: String): ApplicationFormAuthToken {
        val token =
            ApplicationFormAuthToken(
                id = UlidCreator.getUlid().toString(),
                applicationFormId = applicationFormId,
                token = generateToken(),
                expiredAt = LocalDateTime.now().plusHours(24),
            )
        applicationFormAuthTokenRepository.save(token)
        return token
    }

    private fun generateToken(): String {
        return buildString {
            repeat(64) {
                append(tokenCharacters[secureRandom.nextInt(tokenCharacters.length)])
            }
        }
    }
}
