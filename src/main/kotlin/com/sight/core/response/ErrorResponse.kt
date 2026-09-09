package com.sight.core.response

data class ErrorResponse(
    val statusCode: Int,
    val message: String,
    val data: Any?,
    val timestamp: Long,
)
