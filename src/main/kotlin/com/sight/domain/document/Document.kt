package com.sight.domain.document

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "khlug_document")
data class Document(
    @Id
    val id: Long,

    @Column(name = "board", nullable = false)
    val board: Long,

    @Column(name = "title", nullable = false)
    val title: String,

    @Column(name = "author", nullable = false)
    val author: Long,

    @Column(name = "state", nullable = false)
    val state: String,

    @Column(name = "count_comment", nullable = false)
    val countComment: Long,

    @Column(name = "count_read", nullable = false)
    val countRead: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime,
)
