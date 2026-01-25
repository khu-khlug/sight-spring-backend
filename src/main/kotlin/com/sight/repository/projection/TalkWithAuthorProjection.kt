package com.sight.repository.projection

import java.time.LocalDateTime

interface TalkWithAuthorProjection {
    val id: Long
    val title: String
    val authorId: Long
    val authorRealname: String
    val createdAt: LocalDateTime
}
