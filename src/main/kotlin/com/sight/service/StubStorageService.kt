package com.sight.service

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

// R2 연동이 비활성화된 환경에서는 외부 스토리지 호출 없이 애플리케이션을 실행한다.
@Service
@ConditionalOnProperty(prefix = "storage.r2", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class StubStorageService : StorageService {
    override fun generateUploadUrl(key: String): String = ""

    override fun isFileExists(key: String): Boolean = true

    override fun deleteFile(key: String) = Unit

    override fun getDownloadUrl(key: String): String = ""

    override fun getDownloadUrl(
        key: String,
        options: FileDownloadOptions,
    ): SignedFileUrl = throw StorageOperationException(StorageFailureReason.TRANSIENT, "파일 저장소가 설정되지 않았습니다.")
}
