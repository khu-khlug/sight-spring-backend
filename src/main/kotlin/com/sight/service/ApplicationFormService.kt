package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.NotFoundException
import com.sight.domain.application.ApplicationComment
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
}
