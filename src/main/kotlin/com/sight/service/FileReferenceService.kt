package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.InternalServerErrorException
import com.sight.core.exception.NotFoundException
import com.sight.domain.file.FileReference
import com.sight.domain.file.FileReferenceType
import com.sight.repository.FileReferenceRepository
import com.sight.repository.FileUploadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FileReferenceService(
    private val references: FileReferenceRepository,
    private val uploads: FileUploadRepository,
    private val deletions: FileUploadDeletionService,
) {
    @Transactional
    fun createFileReference(
        fileUploadId: String,
        referenceType: FileReferenceType,
        ownerId: String,
    ): FileReference {
        if (referenceType.isSingleOwnerFile && getSingleFileReferenceByOwner(referenceType, ownerId) != null) {
            throw BadRequestException("소유자에게 파일 참조가 이미 있습니다.")
        }
        return addReference(fileUploadId, referenceType, ownerId)
    }

    private fun addReference(
        fileUploadId: String,
        referenceType: FileReferenceType,
        ownerId: String,
    ): FileReference {
        val file = uploads.findForUpdateById(fileUploadId) ?: throw NotFoundException("파일을 찾을 수 없습니다.")
        if (file.isDeleted) throw BadRequestException("삭제 처리된 파일은 참조할 수 없습니다.")
        return references.save(FileReference(UlidCreator.getUlid().toString(), fileUploadId, referenceType, ownerId))
    }

    @Transactional(readOnly = true)
    fun getFileReferenceById(
        id: String,
        referenceType: FileReferenceType,
        ownerId: String,
    ): FileReference {
        val reference = references.findById(id) ?: throw NotFoundException("파일 참조를 찾을 수 없습니다.")
        if (reference.referenceType != referenceType || reference.ownerId != ownerId) {
            throw NotFoundException("파일 참조를 찾을 수 없습니다.")
        }
        return reference
    }

    @Transactional(readOnly = true)
    fun getSingleFileReferenceByOwner(
        referenceType: FileReferenceType,
        ownerId: String,
    ): FileReference? {
        if (!referenceType.isSingleOwnerFile) throw BadRequestException("단일 파일 참조 종류가 아닙니다.")
        val found = references.findByTypeAndOwnerOrderById(referenceType, ownerId, "", 2)
        if (found.size > 1) throw InternalServerErrorException("단일 파일 소유자의 참조가 중복되어 있습니다.")
        return found.firstOrNull()
    }

    // 기존 소유자의 참조 변경은 소유자 행을 먼저 잠근 트랜잭션에서 수행한다.
    @Transactional
    fun replaceSingleFileReference(
        fileUploadId: String,
        referenceType: FileReferenceType,
        ownerId: String,
    ): FileReference {
        val previous = getSingleFileReferenceByOwner(referenceType, ownerId)
        if (previous?.fileUploadId == fileUploadId) {
            val file = uploads.findForUpdateById(fileUploadId) ?: throw NotFoundException("파일을 찾을 수 없습니다.")
            if (file.isDeleted) throw BadRequestException("삭제 처리된 파일은 참조할 수 없습니다.")
            return previous
        }
        val created = addReference(fileUploadId, referenceType, ownerId)
        if (previous != null) deleteFileReference(previous.id, referenceType, ownerId)
        return created
    }

    @Transactional
    fun deleteSingleFileReferenceByOwner(
        referenceType: FileReferenceType,
        ownerId: String,
    ) {
        val reference = getSingleFileReferenceByOwner(referenceType, ownerId) ?: return
        deleteFileReference(reference.id, referenceType, ownerId)
    }

    @Transactional(readOnly = true)
    fun listFileReferences(
        referenceType: FileReferenceType,
        ownerId: String,
        cursor: String = "",
        limit: Int = 100,
    ): List<FileReference> {
        if (limit !in 1..100) throw BadRequestException("조회 개수는 1~100이어야 합니다.")
        return references.findByTypeAndOwnerOrderById(referenceType, ownerId, cursor, limit)
    }

    @Transactional
    fun deleteFileReference(
        id: String,
        referenceType: FileReferenceType,
        ownerId: String,
    ) {
        val reference = references.findById(id) ?: return
        if (reference.referenceType != referenceType || reference.ownerId != ownerId) {
            throw NotFoundException("파일 참조를 찾을 수 없습니다.")
        }
        uploads.findForUpdateById(reference.fileUploadId) ?: return
        if (references.deleteByIdAndTypeAndOwner(id, referenceType, ownerId)) deletions.deleteFileUpload(reference.fileUploadId)
    }
}
