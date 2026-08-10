package com.sight.controllers.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.controllers.http.dto.CreateSmsMessageRequest
import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.domain.sms.SmsMessageType
import com.sight.service.CreateSmsMessagesResult
import com.sight.service.SmsMessageResult
import com.sight.service.SmsMessageResultStatus
import com.sight.service.SmsMessageService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    SmsMessageController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class)
@EnableAspectJAutoProxy
class SmsMessageControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var smsMessageService: SmsMessageService

    @BeforeEach
    fun setUp() {
        authenticate(UserRole.MANAGER)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `모든 수신자가 정상 접수되면 200 OK와 결과를 반환한다`() {
        val request = request()
        given(
            smsMessageService.createSmsMessages(
                request.memberIds!!.filterNotNull(),
                request.additionalPhoneNumbers!!.filterNotNull(),
                request.message!!,
            ),
        )
            .willReturn(
                CreateSmsMessagesResult(
                    results =
                        listOf(
                            SmsMessageResult(
                                memberId = 1L,
                                phone = "01011112222",
                                type = SmsMessageType.SMS,
                                status = SmsMessageResultStatus.SENT,
                                message = null,
                            ),
                        ),
                ),
            )

        mockMvc.perform(
            post("/manager/sms-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results[0].memberId").value(1))
            .andExpect(jsonPath("$.results[0].phone").value("01011112222"))
            .andExpect(jsonPath("$.results[0].type").value("SMS"))
            .andExpect(jsonPath("$.results[0].status").value("SENT"))
            .andExpect(jsonPath("$.results[0].message").isEmpty)
    }

    @Test
    fun `실패하거나 건너뛴 수신자가 있으면 422와 수신자별 결과를 반환한다`() {
        val request = request()
        given(
            smsMessageService.createSmsMessages(
                request.memberIds!!.filterNotNull(),
                request.additionalPhoneNumbers!!.filterNotNull(),
                request.message!!,
            ),
        )
            .willReturn(
                CreateSmsMessagesResult(
                    results =
                        listOf(
                            SmsMessageResult(
                                memberId = 1L,
                                phone = null,
                                type = null,
                                status = SmsMessageResultStatus.SKIPPED,
                                message = "회원에게 등록된 전화번호가 없어 발송하지 않았습니다",
                            ),
                        ),
                ),
            )

        mockMvc.perform(
            post("/manager/sms-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.results[0].memberId").value(1))
            .andExpect(jsonPath("$.results[0].phone").isEmpty)
            .andExpect(jsonPath("$.results[0].type").isEmpty)
            .andExpect(jsonPath("$.results[0].status").value("SKIPPED"))
            .andExpect(jsonPath("$.results[0].message").isNotEmpty)
    }

    @Test
    fun `필수 수신자 목록이 누락되면 400 Bad Request를 반환한다`() {
        mockMvc.perform(
            post("/manager/sms-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"message":"안내 메시지"}"""),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `수신자 목록 원소가 null이면 400 Bad Request를 반환한다`() {
        mockMvc.perform(
            post("/manager/sms-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "memberIds": [null],
                      "additionalPhoneNumbers": [null],
                      "message": "안내 메시지"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `공백 메시지를 받으면 400 Bad Request를 반환한다`() {
        val request = request().copy(message = "   ")

        mockMvc.perform(
            post("/manager/sms-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `일반 회원은 문자 발송을 요청할 수 없다`() {
        authenticate(UserRole.USER)
        val request = request()

        mockMvc.perform(
            post("/manager/sms-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `인증되지 않은 요청은 문자 발송을 요청할 수 없다`() {
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")),
            )
        val request = request()

        mockMvc.perform(
            post("/manager/sms-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `운영진의 요청 필드를 서비스 인자로 전달한다`() {
        val request = request()
        given(
            smsMessageService.createSmsMessages(
                request.memberIds!!.filterNotNull(),
                request.additionalPhoneNumbers!!.filterNotNull(),
                request.message!!,
            ),
        )
            .willReturn(CreateSmsMessagesResult(emptyList()))

        mockMvc.perform(
            post("/manager/sms-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)

        verify(smsMessageService).createSmsMessages(
            memberIds = listOf(1L),
            additionalPhoneNumbers = listOf("010-3333-4444"),
            message = "안녕하세요 {realname}",
        )
    }

    private fun request(): CreateSmsMessageRequest =
        CreateSmsMessageRequest(
            memberIds = listOf(1L),
            additionalPhoneNumbers = listOf("010-3333-4444"),
            message = "안녕하세요 {realname}",
        )

    private fun authenticate(role: UserRole) {
        val requester = Requester(userId = 1L, role = role)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                requester,
                null,
                listOf(SimpleGrantedAuthority("ROLE_${role.name}")),
            )
    }
}
