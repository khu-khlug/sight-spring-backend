package com.sight.service.sms

import com.fasterxml.jackson.annotation.JsonInclude
import com.sight.config.SolapiProperties
import com.sight.domain.sms.SmsMessageType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.HexFormat
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class SolapiMessageClient(
    @Qualifier("solapiRestTemplate")
    private val restTemplate: RestTemplate,
    private val properties: SolapiProperties,
) : SmsMessageClient {
    private val logger = LoggerFactory.getLogger(SolapiMessageClient::class.java)

    override fun send(messages: List<SendSmsMessage>): List<SmsMessageClientResult> {
        if (messages.isEmpty()) return emptyList()
        if (properties.apiKey.isBlank() || properties.apiSecret.isBlank()) {
            throw SmsMessageClientException("문자 발송 서비스 인증 정보가 설정되지 않았습니다")
        }

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(HttpHeaders.AUTHORIZATION, createAuthorizationHeader(Instant.now().toString(), newSalt()))
            }
        val request =
            HttpEntity(
                SolapiSendRequest(
                    messages = messages.map { it.toSolapiRequest() },
                ),
                headers,
            )

        val response =
            try {
                restTemplate.exchange(
                    "${properties.baseUrl.trimEnd('/')}/messages/v4/send-many/detail",
                    HttpMethod.POST,
                    request,
                    SolapiSendResponse::class.java,
                )
            } catch (e: HttpStatusCodeException) {
                logger.error("SOLAPI 문자 발송 요청 실패: status={}", e.statusCode.value())
                throw SmsMessageClientException("문자 발송 서비스 요청에 실패했습니다")
            } catch (e: RestClientException) {
                logger.error("SOLAPI 문자 발송 통신 실패: type={}", e.javaClass.simpleName)
                throw SmsMessageClientException("문자 발송 서비스와 통신할 수 없습니다")
            }

        val body = response.body ?: throw SmsMessageClientException("문자 발송 서비스 응답을 확인할 수 없습니다")
        return mapResponse(messages, body)
    }

    internal fun createAuthorizationHeader(
        date: String,
        salt: String,
    ): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(properties.apiSecret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val signature =
            HexFormat.of().formatHex(
                mac.doFinal((date + salt).toByteArray(StandardCharsets.UTF_8)),
            )
        return "HMAC-SHA256 apiKey=${properties.apiKey}, date=$date, salt=$salt, signature=$signature"
    }

    private fun newSalt(): String = UUID.randomUUID().toString().replace("-", "")

    private fun SendSmsMessage.toSolapiRequest(): SolapiMessageRequest =
        SolapiMessageRequest(
            from = from,
            to = to,
            text = text,
            type = type,
            autoTypeDetect = false,
            subject = if (type == SmsMessageType.LMS) LMS_SUBJECT else null,
            customFields = mapOf(RECIPIENT_INDEX_FIELD to recipientIndex),
        )

    private fun mapResponse(
        messages: List<SendSmsMessage>,
        response: SolapiSendResponse,
    ): List<SmsMessageClientResult> {
        val acceptedIndexes = response.messageList.map { it.recipientIndex() }
        val failedIndexes = response.failedMessageList.map { it.recipientIndex() }
        val responseIndexes = acceptedIndexes + failedIndexes
        val requestIndexes = messages.map { it.recipientIndex }

        if (
            requestIndexes.size != requestIndexes.toSet().size ||
            responseIndexes.size != responseIndexes.toSet().size ||
            responseIndexes.toSet() != requestIndexes.toSet()
        ) {
            throw SmsMessageClientException("문자 발송 서비스의 수신자별 응답을 확인할 수 없습니다")
        }

        val acceptedIndexSet = acceptedIndexes.toSet()
        return requestIndexes.map { recipientIndex ->
            SmsMessageClientResult(
                recipientIndex = recipientIndex,
                accepted = recipientIndex in acceptedIndexSet,
            )
        }
    }

    private fun SolapiMessageResult.recipientIndex(): String =
        customFields?.get(RECIPIENT_INDEX_FIELD)
            ?: throw SmsMessageClientException("문자 발송 서비스의 수신자 식별값을 확인할 수 없습니다")

    private data class SolapiSendRequest(
        val messages: List<SolapiMessageRequest>,
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class SolapiMessageRequest(
        val from: String,
        val to: String,
        val text: String,
        val type: SmsMessageType,
        val autoTypeDetect: Boolean,
        val subject: String?,
        val customFields: Map<String, String>,
    )

    private data class SolapiSendResponse(
        val messageList: List<SolapiMessageResult> = emptyList(),
        val failedMessageList: List<SolapiMessageResult> = emptyList(),
    )

    private data class SolapiMessageResult(
        val customFields: Map<String, String>? = null,
    )

    companion object {
        private const val LMS_SUBJECT = "쿠러그, 경희대학교 중앙 IT 동아리"
        private const val RECIPIENT_INDEX_FIELD = "recipientIndex"
    }
}
