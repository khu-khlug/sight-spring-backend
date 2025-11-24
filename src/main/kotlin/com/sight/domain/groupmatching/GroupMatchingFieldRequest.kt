package com.sight.domain.groupmatching

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "group_matching_field_request")
data class GroupMatchingFieldRequest(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "requester_user_id", nullable = false)
    val requesterUserId: Long,

    @Column(name = "field_name", nullable = false, length = 255)
    val fieldName: String,

    @Column(name = "request_reason", length = 1000)
    val requestReason: String? = null,

    @Column(name = "approved_at")
    val approvedAt: LocalDateTime? = null,

    @Column(name = "rejected_at")
    val rejectedAt: LocalDateTime? = null,

    @Column(name = "reject_reason")
    val rejectReason: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
