package com.sight.service

interface StorageService {
    fun generateUploadUrl(key: String): String

    fun isFileExists(key: String): Boolean

    // 이미 없는 객체는 성공 또는 NO_SUCH_KEY로 처리하며, 버킷·인증 오류와 구분한다.
    fun deleteFile(key: String)

    fun getDownloadUrl(key: String): String
}
