package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class ListTalksResponse(
    val count: Long,
    val talks: List<TalkResponse>,
)

data class TalkResponse(
    val id: Long,
    val title: String,
    val author: TalkAuthorResponse,
    val createdAt: LocalDateTime,
)

data class TalkAuthorResponse(
    val id: Long,
    val realname: String,
)
