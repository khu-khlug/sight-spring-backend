package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnauthorizedException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.core.info21.Info21AuthClient
import com.sight.core.info21.Info21AuthRequest
import com.sight.domain.application.ApplicationComment
import com.sight.domain.application.ApplicationContent
import com.sight.domain.application.ApplicationForm
import com.sight.domain.application.ApplicationFormAuthToken
import com.sight.domain.application.ApplicationFormStatus
import com.sight.domain.application.ApplicationQuestion
import com.sight.repository.ApplicationCommentRepository
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
    private val applicationCommentRepository: ApplicationCommentRepository,
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
    fun createComment(
        applicationFormId: String,
        authorUserId: Long,
        content: String,
    ): ApplicationComment {
        applicationFormRepository.findById(applicationFormId).orElseThrow {
            NotFoundException("가입신청서를 찾을 수 없습니다: $applicationFormId")
        }

        val comment =
            ApplicationComment(
                id = UlidCreator.getUlid().toString(),
                applicationFormId = applicationFormId,
                authorUserId = authorUserId,
                content = content,
            )

        return applicationCommentRepository.save(comment)
    }

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

    @Transactional
    fun passApplicationForm(
        applicationFormId: String,
        authorUserId: Long,
    ): ApplicationForm {
        // 1. 주어진 applicationFormId로 ApplicationForm 엔티티를 조회합니다. 존재하지 않으면 404.
        val applicationForm =
            applicationFormRepository.findById(applicationFormId).orElseThrow {
                NotFoundException("가입신청서를 찾을 수 없습니다: $applicationFormId")
            }

        // 2. 조회한 가입신청서의 status가 제출됨 상태인지 확인합니다. 이미 합격/불합격/중단 상태라면 422.
        if (applicationForm.status != ApplicationFormStatus.SUBMITTED) {
            throw UnprocessableEntityException("제출된 상태의 가입신청서만 합격 처리할 수 있습니다: ${applicationForm.status}")
        }

        // 3. ApplicationComment 객체를 생성합니다.(id = ulid, applicationFormId = path parameter, authorUserId = 요청한 운영진 유저 ID, content = “가입신청서가 합격 처리되었습니다.”)
        val comment =
            ApplicationComment(
                id = UlidCreator.getUlid().toString(),
                applicationFormId = applicationFormId,
                authorUserId = authorUserId,
                content = "가입신청서가 합격 처리되었습니다.",
            )

        // 4. 생성한 댓글을 저장합니다.
        applicationCommentRepository.save(comment)

        // 5. 해당 가입신청서의 status를 합격으로 변경합니다.
        // 6. 쿠러그 회원 정보를 생성합니다.
        createKhlugMember(applicationForm)

        // 7. 저장합니다.
        val updatedForm = applicationForm.copy(status = ApplicationFormStatus.PASSED)
        return applicationFormRepository.save(updatedForm)
    }

    private fun createKhlugMember(applicationForm: ApplicationForm) {
        // TODO: 쿠러그 회원 정보 생성
        // 합격 처리 시 쿠러그 회원(khlug_members) 정보를 생성해야 합니다.
    }

    @Transactional
    fun rejectApplicationForm(
        applicationFormId: String,
        authorUserId: Long,
    ): ApplicationForm {
        // 1. 주어진 applicationFormId로 ApplicationForm 엔티티를 조회합니다. 존재하지 않으면 404.
        val applicationForm =
            applicationFormRepository.findById(applicationFormId).orElseThrow {
                NotFoundException("가입신청서를 찾을 수 없습니다: $applicationFormId")
            }

        // 2. 조회한 가입신청서의 status가 제출됨 상태인지 확인합니다. 이미 합격/불합격/중단 상태라면 422.
        if (applicationForm.status != ApplicationFormStatus.SUBMITTED) {
            throw UnprocessableEntityException("제출된 상태의 가입신청서만 불합격 처리할 수 있습니다: ${applicationForm.status}")
        }

        // 3. ApplicationComment 객체를 생성합니다.(id = ulid, applicationFormId = path parameter, authorUserId = 요청한 운영진 유저 ID, content = “가입신청서가 불합격 처리되었습니다.”)
        val comment =
            ApplicationComment(
                id = UlidCreator.getUlid().toString(),
                applicationFormId = applicationFormId,
                authorUserId = authorUserId,
                content = "가입신청서가 불합격 처리되었습니다.",
            )

        // 4. 생성한 댓글을 저장합니다.
        applicationCommentRepository.save(comment)

        // 5. 해당 가입신청서의 status를 불합격으로 변경합니다.
        val updatedForm = applicationForm.copy(status = ApplicationFormStatus.REJECTED)

        // 6. 저장합니다.
        return applicationFormRepository.save(updatedForm)
    }
}
