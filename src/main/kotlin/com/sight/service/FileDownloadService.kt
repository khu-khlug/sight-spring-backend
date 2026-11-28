package com.sight.service

import com.sight.core.exception.BadGatewayException
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.file.FileUpload
import com.sight.repository.FileUploadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.util.MimeTypeUtils
import java.time.Instant

@Service
class FileDownloadService(
    private val uploads: FileUploadRepository,
    private val storage: StorageService,
    transactionManager: PlatformTransactionManager,
) {
    private val readTransaction = TransactionTemplate(transactionManager).apply { isReadOnly = true }
    private val writeTransaction = TransactionTemplate(transactionManager)

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun getDownloadUrl(
        fileId: String,
        isAllowed: (String) -> Boolean,
    ): String {
        if (!FileUpload.isValidId(fileId)) throw BadRequestException("파일 아이디 형식이 올바르지 않습니다.")
        val fileKey =
            requireNotNull(
                readTransaction.execute {
                    requireDownloadableFile(uploads.findById(fileId).orElse(null), isAllowed).fileKey
                },
            )
        try {
            if (!storage.isFileExists(fileKey)) throw NotFoundException("파일을 찾을 수 없습니다.")
            return requireNotNull(
                writeTransaction.execute {
                    val file = requireDownloadableFile(uploads.findForUpdateById(fileId), isAllowed)
                    if (file.fileKey != fileKey) throw NotFoundException("파일을 찾을 수 없습니다.")
                    file.reusableFileUrl(Instant.now()) ?: issueFileUrl(file)
                },
            )
        } catch (e: StorageOperationException) {
            if (e.reason == StorageFailureReason.NO_SUCH_KEY) throw NotFoundException("파일을 찾을 수 없습니다.")
            throw BadGatewayException("파일 저장소에서 읽기 주소를 가져올 수 없습니다.")
        }
    }

    private fun requireDownloadableFile(
        upload: FileUpload?,
        isAllowed: (String) -> Boolean,
    ): FileUpload =
        upload?.takeIf { it.isUploaded && !it.isDeleted && isAllowed(it.id) }
            ?: throw NotFoundException("파일을 찾을 수 없습니다.")

    private fun issueFileUrl(file: FileUpload): String {
        val contentType = resolveContentType(file.contentType)
        val signed =
            storage.getDownloadUrl(
                file.fileKey,
                FileDownloadOptions(
                    fileName = file.fileName.orEmpty().replace(UNSAFE_FILE_NAME_CHARS, "_").ifBlank { file.id },
                    contentType = contentType,
                    inline = contentType in INLINE_CONTENT_TYPES,
                ),
            )
        file.cacheFileUrl(signed.url, signed.expiresAt, Instant.now())
        return signed.url
    }

    private fun resolveContentType(value: String?): String {
        val mimeType =
            try {
                value?.let(MimeTypeUtils::parseMimeType)
            } catch (_: IllegalArgumentException) {
                null
            }
        return mimeType?.takeIf { !it.isWildcardType && !it.isWildcardSubtype }
            ?.let { "${it.type}/${it.subtype}" } ?: "application/octet-stream"
    }

    private companion object {
        val INLINE_CONTENT_TYPES =
            setOf(
                "image/jpeg", "image/png", "image/webp", "image/gif", "image/avif",
                "audio/mpeg", "audio/mp4", "audio/aac", "audio/ogg", "audio/wav", "audio/flac", "audio/webm",
                "video/mp4", "video/webm",
            )
        val UNSAFE_FILE_NAME_CHARS = Regex("[\\p{Cc}/\\\\]")
    }
}
