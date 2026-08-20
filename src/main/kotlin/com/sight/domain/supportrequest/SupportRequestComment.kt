package com.sight.domain.supportrequest

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.Instant

@Entity
@Table(name = "support_request_comment")
class SupportRequestComment(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "support_request_id",
        nullable = false,
        columnDefinition = "VARCHAR(100)",
        foreignKey = ForeignKey(name = "fk_support_request_comment_request"),
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    val supportRequest: SupportRequest,
    @Column(name = "author_id", nullable = false)
    val authorId: Long,
    @Lob
    @Column(name = "content", nullable = false)
    val content: String,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
