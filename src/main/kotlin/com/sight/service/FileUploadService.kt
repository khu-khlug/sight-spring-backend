package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.file.FileUpload
import com.sight.repository.FileUploadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class UploadLinkResult(
    val url: String,
    val fileKey: String,
    val fileUploadId: String,
)

@Service
class FileUploadService(
    private val fileUploadRepository: FileUploadRepository,
    private val storageService: StorageService,
) {
    @Transactional
    fun createUploadLink(
        memberId: Long,
        apiPath: String,
    ): UploadLinkResult {
        val fileKey = UlidCreator.getUlid().toString()
        val url = storageService.generateUploadUrl(fileKey)
        val fileUpload =
            fileUploadRepository.save(
                FileUpload(
                    id = UlidCreator.getUlid().toString(),
                    fileKey = fileKey,
                    uploaderMemberId = memberId,
                    apiPath = apiPath,
                    fileName = null,
                    size = null,
                    contentType = null,
                    createdAt = Instant.now(),
                ),
            )
        return UploadLinkResult(url = url, fileKey = fileKey, fileUploadId = fileUpload.id)
    }

    @Transactional
    fun validateFileUpload(
        fileUploadId: String,
        memberId: Long,
        apiPath: String,
    ): FileUpload {
        val fileUpload =
            fileUploadRepository.findById(fileUploadId)
                .orElseThrow { NotFoundException("파일을 찾을 수 없습니다.") }
        if (fileUpload.apiPath != apiPath) throw UnprocessableEntityException("해당 경로에서 발급된 파일 업로드 아이디가 아닙니다.")
        if (!fileUpload.isIssuedBy(memberId)) throw ForbiddenException("링크 발급자가 아닙니다.")
        if (fileUpload.isUploaded) throw UnprocessableEntityException("이미 사용된 업로드 링크입니다.")
        if (fileUpload.isDeleted) throw UnprocessableEntityException("삭제 처리된 파일입니다.")
        if (!storageService.isFileExists(fileUpload.fileKey)) {
            throw NotFoundException("파일을 찾을 수 없습니다.")
        }
        return fileUpload
    }

    @Transactional
    fun markAsUsed(fileUpload: FileUpload) {
        fileUpload.markUploaded()
        fileUploadRepository.save(fileUpload)
    }

    @Transactional(readOnly = true)
    fun getFileUploadById(fileUploadId: String): FileUpload =
        fileUploadRepository.findById(fileUploadId)
            .orElseThrow { NotFoundException("파일을 찾을 수 없습니다.") }

    @Transactional
    fun deleteFileUpload(fileUploadId: String) {
        fileUploadRepository.deleteById(fileUploadId)
    }
}
