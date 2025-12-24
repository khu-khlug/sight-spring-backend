package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class ListGroupMatchingsResponse(
    val count: Int,
    val groupMatchings: List<GroupMatchingResponse>,
) {
    data class GroupMatchingResponse(
        val id: String,
        val year: Int,
        val semester: Int,
        val closedAt: LocalDateTime,
        val createdAt: LocalDateTime,
    )
}
