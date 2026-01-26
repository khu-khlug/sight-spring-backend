package com.sight.repository.projection

import java.time.LocalDateTime

interface GroupListProjection {
    val id: Long
    val category: String
    val title: String
    val state: String
    val countMember: Long
    val allowJoin: Byte
    val createdAt: LocalDateTime
}
