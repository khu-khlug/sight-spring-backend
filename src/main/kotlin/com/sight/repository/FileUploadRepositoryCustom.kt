package com.sight.repository

import com.sight.domain.file.FileUpload

interface FileUploadRepositoryCustom {
    fun findForUpdateById(id: String): FileUpload?
}
