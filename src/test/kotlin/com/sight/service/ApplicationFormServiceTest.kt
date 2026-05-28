package com.sight.service

import com.sight.core.exception.UnauthorizedException
import com.sight.core.info21.Info21AuthClient
import com.sight.core.info21.StuauthData
import com.sight.core.info21.StuauthMajor
import com.sight.core.info21.StuauthResponse
import com.sight.domain.application.ApplicationContent
import com.sight.domain.application.ApplicationForm
import com.sight.domain.application.ApplicationFormStatus
import com.sight.domain.application.ApplicationQuestion
import com.sight.domain.application.InterviewAvailableTime
import com.sight.repository.ApplicationContentRepository
import com.sight.repository.ApplicationFormAuthTokenRepository
import com.sight.repository.ApplicationFormRepository
import com.sight.repository.ApplicationQuestionRepository
import com.sight.repository.InterviewAvailableTimeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.LocalDateTime

class ApplicationFormServiceTest {
    private val info21AuthClient = mock<Info21AuthClient>()
    private val applicationFormRepository = mock<ApplicationFormRepository>()
    private val applicationQuestionRepository = mock<ApplicationQuestionRepository>()
    private val applicationContentRepository = mock<ApplicationContentRepository>()
    private val applicationFormAuthTokenRepository = mock<ApplicationFormAuthTokenRepository>()
    private val interviewAvailableTimeRepository = mock<InterviewAvailableTimeRepository>()
    private lateinit var service: ApplicationFormService

    @BeforeEach
    fun setUp() {
        service =
            ApplicationFormService(
                info21AuthClient = info21AuthClient,
                applicationFormRepository = applicationFormRepository,
                applicationQuestionRepository = applicationQuestionRepository,
                applicationContentRepository = applicationContentRepository,
                applicationFormAuthTokenRepository = applicationFormAuthTokenRepository,
                interviewAvailableTimeRepository = interviewAvailableTimeRepository,
            )
    }

    @Test
    fun `createDraft는 Info21 인증 실패 시 UnauthorizedException을 던진다`() {
        given(info21AuthClient.authenticate(any()))
            .willReturn(stuauthResponse(code = 401))

        assertThrows<UnauthorizedException> {
            service.createDraft("2021999999", "wrong-password")
        }
    }

    @Test
    fun `createDraft는 기존 임시저장 신청서가 있으면 신청서를 새로 만들지 않고 토큰을 발급한다`() {
        val applicationForm =
            ApplicationForm(
                id = "form-1",
                info21Id = "2021999999",
                submittee = "김테스트",
                status = ApplicationFormStatus.DRAFT,
            )
        val content =
            ApplicationContent(
                id = "content-1",
                applicationFormId = applicationForm.id,
                questionId = "question-1",
                content = "기존 답변",
            )
        val availableTime =
            InterviewAvailableTime(
                id = "time-1",
                applicationFormId = applicationForm.id,
                availableAt = "2026-06-01 10:00",
            )

        given(info21AuthClient.authenticate(any())).willReturn(stuauthResponse())
        given(
            applicationFormRepository.findFirstByInfo21IdAndStatusInOrderByUpdatedAtDesc(
                eq("2021999999"),
                eq(listOf(ApplicationFormStatus.DRAFT, ApplicationFormStatus.SUBMITTED)),
            ),
        ).willReturn(applicationForm)
        given(applicationContentRepository.findAllByApplicationFormId(applicationForm.id)).willReturn(listOf(content))
        given(interviewAvailableTimeRepository.findAllByApplicationFormId(applicationForm.id))
            .willReturn(listOf(availableTime))

        val result = service.createDraft("2021999999", "password")

        assertEquals(applicationForm.id, result.id)
        assertEquals("기존 답변", result.contents.single().content)
        assertEquals("2026-06-01", result.interviewAvailableTimes.single().date)
        assertEquals("10:00", result.interviewAvailableTimes.single().time)
        assertEquals(64, result.token.length)
        verify(applicationFormRepository, never()).save(any())
        verify(applicationContentRepository, never()).saveAll(any<List<ApplicationContent>>())
        verify(applicationFormAuthTokenRepository).save(any())
    }

    @Test
    fun `createDraft는 기존 신청서가 없으면 노출 질문 기준으로 초안을 생성한다`() {
        val firstQuestion =
            ApplicationQuestion(
                id = "question-1",
                title = "자기소개",
                description = "자기소개를 입력해주세요",
                minLength = 10,
                order = 2,
                isExposed = true,
                createdAt = LocalDateTime.now().minusDays(1),
            )
        val secondQuestion =
            ApplicationQuestion(
                id = "question-2",
                title = "지원동기",
                description = "지원동기를 입력해주세요",
                minLength = 10,
                order = 1,
                isExposed = true,
            )
        val nullOrderQuestion =
            ApplicationQuestion(
                id = "question-3",
                title = "기타",
                description = "기타 내용을 입력해주세요",
                minLength = 0,
                order = null,
                isExposed = true,
            )

        given(info21AuthClient.authenticate(any())).willReturn(stuauthResponse(name = "김테스트"))
        given(
            applicationFormRepository.findFirstByInfo21IdAndStatusInOrderByUpdatedAtDesc(
                eq("2021999999"),
                eq(listOf(ApplicationFormStatus.DRAFT, ApplicationFormStatus.SUBMITTED)),
            ),
        ).willReturn(null)
        given(applicationQuestionRepository.findAllByIsExposedTrue())
            .willReturn(listOf(firstQuestion, nullOrderQuestion, secondQuestion))
        given(applicationContentRepository.findAllByApplicationFormId(any())).willReturn(emptyList())
        given(interviewAvailableTimeRepository.findAllByApplicationFormId(any())).willReturn(emptyList())

        val result = service.createDraft("2021999999", "password")

        val formCaptor = argumentCaptor<ApplicationForm>()
        val contentsCaptor = argumentCaptor<List<ApplicationContent>>()
        verify(applicationFormRepository).save(formCaptor.capture())
        verify(applicationContentRepository).saveAll(contentsCaptor.capture())
        verify(applicationFormAuthTokenRepository).save(any())

        assertEquals("2021999999", formCaptor.firstValue.info21Id)
        assertEquals("김테스트", formCaptor.firstValue.submittee)
        assertEquals(ApplicationFormStatus.DRAFT, formCaptor.firstValue.status)
        assertEquals(formCaptor.firstValue.id, result.id)
        assertEquals(64, result.token.length)
        assertTrue(result.contents.isEmpty())
        assertEquals(listOf("question-2", "question-1", "question-3"), contentsCaptor.firstValue.map { it.questionId })
        assertTrue(contentsCaptor.firstValue.all { it.content == "" })
    }

    private fun stuauthResponse(
        code: Int = 200,
        name: String = "김테스트",
    ): StuauthResponse {
        return StuauthResponse(
            code = code,
            message = "OK",
            data =
                StuauthData(
                    studentNumber = 2021999999,
                    name = name,
                    grade = 1,
                    major =
                        listOf(
                            StuauthMajor(
                                college = "소프트웨어융합대학",
                                department = "컴퓨터공학부",
                            ),
                        ),
                    phone = "010-0000-0000",
                ),
        )
    }
}
