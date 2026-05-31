package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotNull

data class AssignApplicationFormManagerRequest(
    @field:NotNull
    val managerUserId: Long?,
)
