package com.sight.controllers.http.dto

import java.time.LocalDate

data class PauseMemberRequest(
    val returnAt: LocalDate,
    val reason: String,
)
