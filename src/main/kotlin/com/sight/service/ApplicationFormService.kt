package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.application.ApplicationComment
import com.sight.domain.application.ApplicationForm
import com.sight.domain.application.ApplicationFormStatus
import com.sight.repository.ApplicationCommentRepository
import com.sight.repository.ApplicationFormRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ApplicationFormService(
    private val applicationFormRepository: ApplicationFormRepository,
    private val applicationCommentRepository: ApplicationCommentRepository,
) {
    @Transactional
    fun createComment(
        applicationFormId: String,
        authorUserId: Long,
        content: String,
    ): ApplicationComment {
        // 1. 주어진 applicationFormId로 ApplicationForm 엔티티를 조회합니다. 존재하지 않으면 404.
        applicationFormRepository.findById(applicationFormId).orElseThrow {
            NotFoundException("가입신청서를 찾을 수 없습니다: $applicationFormId")
        }

        // 2. ApplicationComment 객체를 생성합니다.(id = ulid, applicationFormId = path parameter, authorUserId = 입력값, content = 입력값)
        val comment =
            ApplicationComment(
                id = UlidCreator.getUlid().toString(),
                applicationFormId = applicationFormId,
                authorUserId = authorUserId,
                content = content,
            )

        // 3. 저장합니다.
        return applicationCommentRepository.save(comment)
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
}
