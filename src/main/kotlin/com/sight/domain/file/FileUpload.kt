package com.sight.domain.file

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "file_upload")
data class FileUpload(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "file_key", nullable = false, length = 255)
    val fileKey: String,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "api_path", nullable = false, length = 255)
    val apiPath: String,

    @Column(name = "is_used", nullable = false, columnDefinition = "TINYINT")
    @ColumnDefault("0")
    val isUsed: Boolean = false,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
