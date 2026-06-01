package com.sight.core.info21

import com.sight.core.exception.InternalServerErrorException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate

// Stuauth는 외부에 API가 직접 노출되지 않아야 합니다.
// Stuauth API의 요청과 응답은 백엔드 내부에서만 처리되어야 하며, API 응답으로 그대로 반환하면 안 됩니다.
@Component
@Profile("prod")
class HttpInfo21AuthClient(
    private val restTemplate: RestTemplate,
    @Value("\${stuauth.api-uri}")
    private val apiUri: String,
    @Value("\${stuauth.token}")
    private val token: String,
) : Info21AuthClient {
    private val logger = LoggerFactory.getLogger(HttpInfo21AuthClient::class.java)

    override fun authenticate(request: Info21AuthRequest): StuauthResponse {
        return try {
            restTemplate.postForObject(
                apiUri,
                HttpEntity(
                    StuauthRequest(
                        token = token,
                        id = request.info21Id,
                        pw = request.info21Password,
                    ),
                ),
                StuauthResponse::class.java,
            ) ?: throw InternalServerErrorException("Stuauth 요청에 실패했습니다")
        } catch (e: HttpStatusCodeException) {
            logger.error("Stuauth request failed: status={}, body={}", e.statusCode, e.responseBodyAsString)
            throw InternalServerErrorException("Stuauth 요청에 실패했습니다")
        } catch (e: Exception) {
            // 요청 정보에 담긴 인증 정보를 로그에 남기지 않기 위해 요청 본문은 로깅하지 않습니다.
            logger.error("Stuauth request failed", e)
            throw InternalServerErrorException("Stuauth 요청에 실패했습니다")
        }
    }

    private data class StuauthRequest(
        val token: String,
        val id: String,
        val pw: String,
    )
}
