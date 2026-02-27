package com.sight.controllers.http.dto

data class GetDoorLockPasswordResponse(
    val master: String,
    val forJajudy: String,
    val forFacilityTeam: String,
)
