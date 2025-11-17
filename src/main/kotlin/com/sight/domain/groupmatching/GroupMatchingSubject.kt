package com.sight.domain.groupmatching

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "group_matching_subject")
data class GroupMatchingSubject(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "answer_id", nullable = false, length = 100)
    val answerId: String,

    @Column(name = "subject", nullable = false, length = 255)
    val subject: String,
)
