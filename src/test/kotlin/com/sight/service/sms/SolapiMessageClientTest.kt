package com.sight.service.sms

import com.sight.config.SolapiProperties
import com.sight.domain.sms.SmsMessageType
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest
import org.springframework.web.client.RestTemplate
import java.net.SocketTimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SolapiMessageClientTest {
    private lateinit var restTemplate: RestTemplate
    private lateinit var server: MockRestServiceServer
    private lateinit var client: SolapiMessageClient

    @BeforeEach
    fun setUp() {
        restTemplate = RestTemplate()
        server = MockRestServiceServer.bindTo(restTemplate).build()
        client =
            SolapiMessageClient(
                restTemplate = restTemplate,
                properties =
                    SolapiProperties(
                        apiKey = "test-api-key",
                        apiSecret = "test-secret",
                        baseUrl = "https://api.solapi.test",
                    ),
            )
    }

    @Test
    fun `HMAC SHA256 인증 헤더를 생성한다`() {
        val header = client.createAuthorizationHeader("2026-08-10T00:00:00Z", "fixed-salt-1234")

        assertEquals(
            "HMAC-SHA256 apiKey=test-api-key, date=2026-08-10T00:00:00Z, salt=fixed-salt-1234, " +
                "signature=56dc0392dcdd89201568881ed0283bb520bdd3e6da9ec127685352e77b579ef4",
            header,
        )
    }

    @Test
    fun `SMS와 LMS를 한 요청으로 보내고 수신자별 결과를 반환한다`() {
        server.expect(once(), requestTo("https://api.solapi.test/messages/v4/send-many/detail"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.AUTHORIZATION, startsWith("HMAC-SHA256 apiKey=test-api-key")))
            .andExpect(
                content().json(
                    """
                    {
                      "messages": [
                        {
                          "from": "029302266",
                          "to": "01011112222",
                          "text": "단문",
                          "type": "SMS",
                          "autoTypeDetect": false,
                          "customFields": {"recipientIndex": "0"}
                        },
                        {
                          "from": "029302266",
                          "to": "01033334444",
                          "text": "장문",
                          "type": "LMS",
                          "autoTypeDetect": false,
                          "subject": "쿠러그, 경희대학교 중앙 IT 동아리",
                          "customFields": {"recipientIndex": "1"}
                        }
                      ]
                    }
                    """.trimIndent(),
                    true,
                ),
            )
            .andRespond(
                withSuccess(
                    """
                    {
                      "messageList": [
                        {"customFields": {"recipientIndex": "0"}}
                      ],
                      "failedMessageList": [
                        {"customFields": {"recipientIndex": "1"}}
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result =
            client.send(
                listOf(
                    message(recipientIndex = "0", type = SmsMessageType.SMS),
                    message(recipientIndex = "1", type = SmsMessageType.LMS, to = "01033334444", text = "장문"),
                ),
            )

        assertTrue(result[0].accepted)
        assertFalse(result[1].accepted)
        server.verify()
    }

    @Test
    fun `수신자 식별 결과가 누락되면 요청 단위 실패로 처리한다`() {
        server.expect(requestTo("https://api.solapi.test/messages/v4/send-many/detail"))
            .andRespond(
                withSuccess(
                    """{"messageList": [], "failedMessageList": []}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        assertThrows<SmsMessageClientException> {
            client.send(listOf(message(recipientIndex = "0", type = SmsMessageType.SMS)))
        }
    }

    @Test
    fun `SOLAPI 비정상 HTTP 응답은 요청 단위 실패로 처리한다`() {
        server.expect(requestTo("https://api.solapi.test/messages/v4/send-many/detail"))
            .andRespond(withUnauthorizedRequest())

        assertThrows<SmsMessageClientException> {
            client.send(listOf(message(recipientIndex = "0", type = SmsMessageType.SMS)))
        }
        server.verify()
    }

    @Test
    fun `SOLAPI timeout은 자동 재시도 없이 요청 단위 실패로 처리한다`() {
        server.expect(once(), requestTo("https://api.solapi.test/messages/v4/send-many/detail"))
            .andRespond(withException(SocketTimeoutException("timeout")))

        assertThrows<SmsMessageClientException> {
            client.send(listOf(message(recipientIndex = "0", type = SmsMessageType.SMS)))
        }
        server.verify()
    }

    @Test
    fun `인증 정보가 비어 있으면 외부 요청을 수행하지 않는다`() {
        val unconfiguredClient =
            SolapiMessageClient(
                restTemplate = restTemplate,
                properties = SolapiProperties(),
            )

        assertThrows<SmsMessageClientException> {
            unconfiguredClient.send(listOf(message(recipientIndex = "0", type = SmsMessageType.SMS)))
        }
        server.verify()
    }

    private fun message(
        recipientIndex: String,
        type: SmsMessageType,
        to: String = "01011112222",
        text: String = "단문",
    ): SendSmsMessage =
        SendSmsMessage(
            recipientIndex = recipientIndex,
            from = "029302266",
            to = to,
            text = text,
            type = type,
        )
}
