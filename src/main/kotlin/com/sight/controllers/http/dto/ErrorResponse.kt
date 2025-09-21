package com.sight.controllers.http.dto

data class ErrorResponse(
    val statusCode: Int,
    val message: String,
    val data: Any?,
    val timestamp: Long,
)
