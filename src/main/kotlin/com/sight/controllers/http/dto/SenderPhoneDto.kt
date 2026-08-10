package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotNull

data class GetSenderPhoneResponse(
    val phone: String,
)

data class UpdateSenderPhoneRequest(
    @field:NotNull(message = "동아리 공식 발신번호는 필수입니다")
    val phone: String?,
)
