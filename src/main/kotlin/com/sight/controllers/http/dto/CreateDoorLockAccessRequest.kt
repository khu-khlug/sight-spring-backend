package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class CreateDoorLockAccessRequest(
    @field:NotNull(message = "학번은 필수입니다")
    @field:Positive(message = "학번은 양수여야 합니다")
    val number: Long?,

    @field:NotNull(message = "방 번호는 필수입니다")
    @field:Positive(message = "방 번호는 양수여야 합니다")
    val roomNumber: Int?,
)
