package com.sight.repository

import com.sight.domain.file.FileUpload
import org.springframework.data.jpa.repository.JpaRepository

interface FileUploadRepository : JpaRepository<FileUpload, String> {
    fun deleteByFileKey(fileKey: String)
}
