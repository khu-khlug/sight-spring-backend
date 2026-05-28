package com.sight.service

interface StorageService {
    fun generateUploadUrl(key: String): String

    fun isFileExists(key: String): Boolean

    fun deleteFile(key: String)

    fun getDownloadUrl(key: String): String
}
