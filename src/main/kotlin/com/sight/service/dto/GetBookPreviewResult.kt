package com.sight.service.dto

data class GetBookPreviewResult(
    val title: String,
    val author: String,
    val coverImageUrl: String,
    val publisher: String,
    val publishedYear: Int,
    val description: String,
)
