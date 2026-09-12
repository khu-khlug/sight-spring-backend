package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class UpdateDoorLockOccupantsRequest(
    @field:NotNull(message = "재실자 목록은 필수입니다")
    @field:Size(max = 24, message = "재실자는 최대 24명까지 전송할 수 있습니다")
    val occupants: List<String?>?,
)
