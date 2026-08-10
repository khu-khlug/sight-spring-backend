package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sight.service.CreateSmsMessagesResult
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateSmsMessageRequest(
    @field:NotNull(message = "회원 수신자 목록은 필수입니다")
    val memberIds: List<Long?>?,
    @field:NotNull(message = "직접 지정 전화번호 목록은 필수입니다")
    val additionalPhoneNumbers: List<String?>?,
    @field:NotBlank(message = "문자 메시지 내용은 공백일 수 없습니다")
    val message: String?,
) {
    @get:JsonIgnore
    @get:AssertTrue(message = "수신자 목록에 null을 포함할 수 없습니다")
    val hasNoNullRecipient: Boolean
        get() =
            memberIds?.none { it == null } != false &&
                additionalPhoneNumbers?.none { it == null } != false
}

data class CreateSmsMessageResponse(
    val results: List<SmsMessageResultResponse>,
) {
    companion object {
        fun from(result: CreateSmsMessagesResult): CreateSmsMessageResponse =
            CreateSmsMessageResponse(
                results =
                    result.results.map {
                        SmsMessageResultResponse(
                            memberId = it.memberId,
                            phone = it.phone,
                            type = it.type?.name,
                            status = it.status.name,
                            message = it.message,
                        )
                    },
            )
    }
}

data class SmsMessageResultResponse(
    val memberId: Long?,
    val phone: String?,
    val type: String?,
    val status: String,
    val message: String?,
)
