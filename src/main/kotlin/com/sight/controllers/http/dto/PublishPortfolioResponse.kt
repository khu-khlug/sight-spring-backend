package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class PublishPortfolioResponse(
    @get:JsonProperty("isPublished")
    val isPublished: Boolean,
)
