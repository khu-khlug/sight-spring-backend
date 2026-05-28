package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.application.ApplicationComment
import com.sight.domain.application.ApplicationForm
import com.sight.domain.application.ApplicationFormStatus
import com.sight.repository.ApplicationCommentRepository
import com.sight.repository.ApplicationFormRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApplicationFormServiceTest {
    private val applicationFormRepository: ApplicationFormRepository = mock()
    private val applicationCommentRepository: ApplicationCommentRepository = mock()
    private lateinit var applicationFormService: ApplicationFormService

    @BeforeEach
    fun setUp() {
        applicationFormService =
            ApplicationFormService(
                applicationFormRepository,
                applicationCommentRepository,
            )
    }

    @Test
    fun `createComment는 가입 신청서가 존재할 때 정상적으로 댓글을 저장하고 반환한다`() {
        // given
        val applicationFormId = "form-ulid"
        val authorUserId = 12345L
        val content = "좋은 지원서네요."

        val applicationForm =
            ApplicationForm(
                id = applicationFormId,
                info21Id = "info21-id",
                submittee = "홍길동",
                status = ApplicationFormStatus.SUBMITTED,
            )

        val savedComment =
            ApplicationComment(
                id = "comment-ulid",
                applicationFormId = applicationFormId,
                authorUserId = authorUserId,
                content = content,
            )

        given(applicationFormRepository.findById(applicationFormId))
            .willReturn(Optional.of(applicationForm))
        given(applicationCommentRepository.save(any<ApplicationComment>()))
            .willReturn(savedComment)

        // when
        val result =
            applicationFormService.createComment(
                applicationFormId = applicationFormId,
                authorUserId = authorUserId,
                content = content,
            )

        // then
        assertNotNull(result)
        assertEquals("comment-ulid", result.id)
        assertEquals(applicationFormId, result.applicationFormId)
        assertEquals(authorUserId, result.authorUserId)
        assertEquals(content, result.content)

        verify(applicationFormRepository).findById(applicationFormId)
        verify(applicationCommentRepository).save(any<ApplicationComment>())
    }

    @Test
    fun `createComment는 존재하지 않는 가입 신청서인 경우 NotFoundException을 던진다`() {
        // given
        val applicationFormId = "non-existent-form"
        val authorUserId = 12345L
        val content = "댓글 내용"

        given(applicationFormRepository.findById(applicationFormId))
            .willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            applicationFormService.createComment(
                applicationFormId = applicationFormId,
                authorUserId = authorUserId,
                content = content,
            )
        }

        verify(applicationFormRepository).findById(applicationFormId)
    }

    @Test
    fun `passApplicationForm는 가입 신청서가 존재하고 제출된 상태일 때 정상적으로 합격 처리한다`() {
        // given
        val applicationFormId = "form-ulid"
        val authorUserId = 12345L

        val applicationForm =
            ApplicationForm(
                id = applicationFormId,
                info21Id = "info21-id",
                submittee = "홍길동",
                status = ApplicationFormStatus.SUBMITTED,
            )

        val savedComment =
            ApplicationComment(
                id = "comment-ulid",
                applicationFormId = applicationFormId,
                authorUserId = authorUserId,
                content = "가입신청서가 합격 처리되었습니다.",
            )

        given(applicationFormRepository.findById(applicationFormId))
            .willReturn(Optional.of(applicationForm))
        given(applicationCommentRepository.save(any<ApplicationComment>()))
            .willReturn(savedComment)
        given(applicationFormRepository.save(any<ApplicationForm>()))
            .willReturn(applicationForm.copy(status = ApplicationFormStatus.PASSED))

        // when
        val result =
            applicationFormService.passApplicationForm(
                applicationFormId = applicationFormId,
                authorUserId = authorUserId,
            )

        // then
        assertNotNull(result)
        assertEquals(ApplicationFormStatus.PASSED, result.status)

        verify(applicationFormRepository).findById(applicationFormId)
        verify(applicationCommentRepository).save(any<ApplicationComment>())
        verify(applicationFormRepository).save(any<ApplicationForm>())
    }

    @Test
    fun `passApplicationForm는 존재하지 않는 가입 신청서인 경우 NotFoundException을 던진다`() {
        // given
        val applicationFormId = "non-existent-form"
        val authorUserId = 12345L

        given(applicationFormRepository.findById(applicationFormId))
            .willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            applicationFormService.passApplicationForm(
                applicationFormId = applicationFormId,
                authorUserId = authorUserId,
            )
        }

        verify(applicationFormRepository).findById(applicationFormId)
    }

    @Test
    fun `passApplicationForm는 가입 신청서가 제출됨 상태가 아닌 경우 UnprocessableEntityException을 던진다`() {
        // given
        val applicationFormId = "form-ulid"
        val authorUserId = 12345L

        val applicationForm =
            ApplicationForm(
                id = applicationFormId,
                info21Id = "info21-id",
                submittee = "홍길동",
                status = ApplicationFormStatus.PASSED,
            )

        given(applicationFormRepository.findById(applicationFormId))
            .willReturn(Optional.of(applicationForm))

        // when & then
        assertThrows<UnprocessableEntityException> {
            applicationFormService.passApplicationForm(
                applicationFormId = applicationFormId,
                authorUserId = authorUserId,
            )
        }

        verify(applicationFormRepository).findById(applicationFormId)
    }
}
