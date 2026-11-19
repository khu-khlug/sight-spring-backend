package com.sight.service

import com.sight.core.exception.BadGatewayException
import com.sight.core.exception.BadRequestException
import com.sight.domain.file.FileUpload
import com.sight.domain.group.GroupFile
import com.sight.repository.FileUploadRepository
import com.sight.repository.GroupFileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

enum class ActivityReportUploadStatus(val rejectionMessage: String?) {
    AVAILABLE(null),
    MISSING("활동보고 파일이 업로드되지 않았습니다."),
    TOO_LARGE("활동보고 파일은 20 MiB 이하여야 합니다."),
}

@Service
class ActivityReportUploadService(
    private val fileUploadRepository: FileUploadRepository,
    private val groupFileRepository: GroupFileRepository,
    private val storageService: StorageService,
    private val fileUploadService: FileUploadService,
    private val integerIdService: IntegerIdService,
) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun createUploadLink(
        groupId: Long,
        memberId: Long,
        size: Long,
    ): UploadLinkResult {
        val link =
            try {
                fileUploadService.createUploadLink(memberId, uploadPath(groupId), size)
            } catch (_: StorageOperationException) {
                throw BadGatewayException("활동보고 업로드 주소를 발급할 수 없습니다.")
            }
        if (link.url.isBlank()) throw BadGatewayException("활동보고 업로드 주소를 발급할 수 없습니다.")
        groupFileRepository.save(GroupFile(id = integerIdService.nextId(), groupId = groupId, fileUploadId = link.fileUploadId))
        return link
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun checkUploadedObject(
        groupId: Long,
        fileId: String,
        memberId: Long,
    ): ActivityReportUploadStatus {
        if (!FileUpload.isValidId(fileId)) throw BadRequestException("파일 아이디 형식이 올바르지 않습니다.")
        val upload = requireAvailable(fileUploadRepository.findById(fileId).orElse(null), groupId, memberId)
        val size =
            try {
                storageService.getFileSize(upload.fileKey)
            } catch (e: StorageOperationException) {
                if (e.reason != StorageFailureReason.NO_SUCH_KEY) throw BadGatewayException("파일 저장소에서 업로드를 확인할 수 없습니다.")
                null
            }
        return when {
            size == null -> ActivityReportUploadStatus.MISSING
            size > FileUpload.MAX_FILE_SIZE -> ActivityReportUploadStatus.TOO_LARGE
            else -> ActivityReportUploadStatus.AVAILABLE
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun confirmUpload(
        groupId: Long,
        fileId: String,
        memberId: Long,
        status: ActivityReportUploadStatus,
    ): FileUpload? {
        val upload = requireAvailable(fileUploadRepository.findForUpdateById(fileId), groupId, memberId)
        if (status == ActivityReportUploadStatus.AVAILABLE) return upload
        fileUploadService.deleteFileUpload(upload.id)
        return null
    }

    private fun requireAvailable(
        file: FileUpload?,
        groupId: Long,
        memberId: Long,
    ): FileUpload =
        file?.takeIf { it.apiPath == uploadPath(groupId) && it.isIssuedBy(memberId) && !it.isUploaded && !it.isDeleted }
            ?: throw BadRequestException("제출에 사용할 수 없는 활동보고 파일입니다.")

    private fun uploadPath(groupId: Long): String = "/groups/$groupId/activity-reports/upload-link"
}
