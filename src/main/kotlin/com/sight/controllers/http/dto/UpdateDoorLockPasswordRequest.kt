package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank

data class UpdateDoorLockPasswordRequest(
    @field:NotBlank val master: String?,
    @field:NotBlank val forJajudy: String?,
    @field:NotBlank val forFacilityTeam: String?,
)
