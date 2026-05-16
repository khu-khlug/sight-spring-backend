package com.sight.service

import org.springframework.stereotype.Service

// R2/S3 클라이언트 결정 전 임시 구현체. 실제 구현 시 교체 필요.
@Service
class StubStorageService : StorageService {
    override fun generateUploadUrl(key: String): String = ""

    override fun isFileExists(key: String): Boolean = true

    override fun deleteFile(key: String) = Unit

    override fun getDownloadUrl(key: String): String = ""
}
