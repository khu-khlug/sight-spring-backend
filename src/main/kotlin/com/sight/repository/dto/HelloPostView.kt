package com.sight.repository.dto

import java.time.Instant

data class HelloPostView(
    val id: Long,
    val category: String?,
    val title: String,
    val date: Instant,
)
