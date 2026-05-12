package com.sight.domain.file

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "r2_file_upload")
data class R2FileUpload(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "r2_key", nullable = false, length = 255)
    val r2Key: String,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "api_path", nullable = false, length = 255)
    val apiPath: String,

    @Column(name = "is_verified", nullable = false, columnDefinition = "TINYINT")
    @ColumnDefault("0")
    val isVerified: Boolean = false,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
