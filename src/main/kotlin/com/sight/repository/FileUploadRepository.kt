package com.sight.repository

import com.sight.domain.file.FileUpload
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface FileUploadRepository : JpaRepository<FileUpload, String>, FileUploadRepositoryCustom {
    @Modifying(flushAutomatically = true)
    @Query(
        value = """
        UPDATE file_upload f SET f.deleted_at = :at, f.read_url = NULL, f.read_url_expires_at = NULL
        WHERE f.id = :id AND f.deleted_at IS NULL
          AND NOT EXISTS (SELECT 1 FROM file_reference r WHERE r.file_upload_id = f.id)
    """,
        nativeQuery = true,
    )
    fun markUnreferencedForDeletion(
        @Param("id") id: String,
        @Param("at") at: Instant,
    ): Int
}
