package com.sight.controllers.http.dto

data class GetBookPreviewResponse(
    val title: String,
    val author: String,
    val coverImageUrl: String,
    val publisher: String,
    val publishedYear: Int,
    val description: String,
)
