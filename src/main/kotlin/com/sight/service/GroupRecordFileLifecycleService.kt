package com.sight.service

import com.sight.repository.FileUploadRepository
import com.sight.repository.GroupFileRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Service
class GroupRecordFileLifecycleService(
    private val uploads: FileUploadRepository,
    private val groupFiles: GroupFileRepository,
    private val storage: StorageService,
    transactionManager: PlatformTransactionManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val transaction =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    // 연결 해제 트랜잭션이 커밋된 뒤 호출한다. 실패한 파일은 삭제 표시를 유지한다.
    fun deleteMarkedUpload(id: String) {
        try {
            val key =
                transaction.execute { uploads.findById(id).orElse(null)?.takeIf { it.isDeleted }?.fileKey }
                    ?: return
            storage.deleteFile(key)
            transaction.executeWithoutResult {
                val file = uploads.findForUpdateById(id)
                if (file != null && file.isDeleted) {
                    groupFiles.deleteByFileUploadId(id)
                    uploads.delete(file)
                }
            }
        } catch (e: Exception) {
            logger.error("기록 첨부 파일 삭제 실패: fileId={}", id, e)
        }
    }
}
