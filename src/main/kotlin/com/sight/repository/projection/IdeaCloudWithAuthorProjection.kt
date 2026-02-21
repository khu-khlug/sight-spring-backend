package com.sight.repository.projection

import java.time.LocalDateTime

interface IdeaCloudWithAuthorProjection {
    val id: Long
    val idea: String
    val authorId: Long
    val authorName: String
    val createdAt: LocalDateTime?
}
