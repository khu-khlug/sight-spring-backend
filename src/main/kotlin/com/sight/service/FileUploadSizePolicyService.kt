package com.sight.service

import com.sight.core.config.ConfigKey
import com.sight.core.config.SystemConfigRegistry
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.InvalidConfigValueException
import com.sight.domain.file.FileReferenceType
import org.springframework.stereotype.Service

@Service
class FileUploadSizePolicyService(
    private val systemConfigRegistry: SystemConfigRegistry,
) {
    fun getMaxSizeBytes(referenceType: FileReferenceType? = null): Long {
        val globalMaxSizeBytes = getConfiguredMaxSizeBytes(ConfigKey.FILE_UPLOAD_MAX_SIZE_BYTES)
        val purposeKey =
            when (referenceType) {
                FileReferenceType.GROUP_RECORD_ATTACHMENT, FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT ->
                    ConfigKey.GROUP_RECORD_FILE_MAX_SIZE_BYTES
                FileReferenceType.GROUP_ACTIVITY_REPORT -> ConfigKey.GROUP_ACTIVITY_REPORT_FILE_MAX_SIZE_BYTES
                FileReferenceType.GROUP_CARD_COVER -> ConfigKey.GROUP_CARD_COVER_IMAGE_MAX_SIZE_BYTES
                null -> return globalMaxSizeBytes
            }
        return minOf(globalMaxSizeBytes, getConfiguredMaxSizeBytes(purposeKey))
    }

    fun validateSize(
        size: Long,
        referenceType: FileReferenceType? = null,
    ) {
        val maxSizeBytes = getMaxSizeBytes(referenceType)
        if (size !in 0..maxSizeBytes) {
            throw BadRequestException("파일 크기는 0바이트 이상 ${maxSizeBytes}바이트 이하여야 합니다.")
        }
    }

    private fun getConfiguredMaxSizeBytes(key: ConfigKey): Long {
        val maxSizeBytes = systemConfigRegistry.getValueAsLong(key)
        if (maxSizeBytes < 0) {
            throw InvalidConfigValueException("파일 최대 크기 설정은 0바이트 이상이어야 합니다. key=$key")
        }
        return maxSizeBytes
    }
}
