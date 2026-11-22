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
        referrerId: String,
    ): FileReference {
        if (referenceType.isSingleFileReferrer && getSingleFileReferenceByReferrer(referenceType, referrerId) != null) {
            throw BadRequestException("참조 개체에 파일 참조가 이미 있습니다.")
        }
        return addReference(fileUploadId, referenceType, referrerId)
    }

    private fun addReference(
        fileUploadId: String,
        referenceType: FileReferenceType,
        referrerId: String,
    ): FileReference {
        val file = uploads.findForUpdateById(fileUploadId) ?: throw NotFoundException("파일을 찾을 수 없습니다.")
        if (file.isDeleted) throw BadRequestException("삭제 처리된 파일은 참조할 수 없습니다.")
        return references.save(FileReference(UlidCreator.getUlid().toString(), fileUploadId, referenceType, referrerId))
    }

    @Transactional(readOnly = true)
    fun getFileReferenceById(
        id: String,
        referenceType: FileReferenceType,
        referrerId: String,
    ): FileReference {
        val reference = references.findById(id) ?: throw NotFoundException("파일 참조를 찾을 수 없습니다.")
        if (reference.referenceType != referenceType || reference.referrerId != referrerId) {
            throw NotFoundException("파일 참조를 찾을 수 없습니다.")
        }
        return reference
    }

    @Transactional(readOnly = true)
    fun getSingleFileReferenceByReferrer(
        referenceType: FileReferenceType,
        referrerId: String,
    ): FileReference? {
        if (!referenceType.isSingleFileReferrer) throw BadRequestException("단일 파일 참조 종류가 아닙니다.")
        val found = references.findByRefTypeAndReferrerOrderById(referenceType, referrerId, "", 2)
        if (found.size > 1) throw InternalServerErrorException("하나의 파일만 참조할 수 있는 개체에 중복 참조가 있습니다.")
        return found.firstOrNull()
    }

    // 기존 참조의 교체는 참조 개체의 행을 먼저 잠근 트랜잭션에서 수행한다.
    @Transactional
    fun replaceSingleFileReference(
        fileUploadId: String,
        referenceType: FileReferenceType,
        referrerId: String,
    ): FileReference {
        val previous = getSingleFileReferenceByReferrer(referenceType, referrerId)
        if (previous?.fileUploadId == fileUploadId) {
            val file = uploads.findForUpdateById(fileUploadId) ?: throw NotFoundException("파일을 찾을 수 없습니다.")
            if (file.isDeleted) throw BadRequestException("삭제 처리된 파일은 참조할 수 없습니다.")
            return previous
        }
        val created = addReference(fileUploadId, referenceType, referrerId)
        if (previous != null) deleteFileReference(previous.id, referenceType, referrerId)
        return created
    }

    @Transactional
    fun deleteSingleFileReferenceByReferrer(
        referenceType: FileReferenceType,
        referrerId: String,
    ) {
        val reference = getSingleFileReferenceByReferrer(referenceType, referrerId) ?: return
        deleteFileReference(reference.id, referenceType, referrerId)
    }

    @Transactional(readOnly = true)
    fun listFileReferences(
        referenceType: FileReferenceType,
        referrerId: String,
        cursor: String = "",
        limit: Int = 100,
    ): List<FileReference> {
        if (limit !in 1..100) throw BadRequestException("조회 개수는 1~100이어야 합니다.")
        return references.findByRefTypeAndReferrerOrderById(referenceType, referrerId, cursor, limit)
    }

    @Transactional
    fun deleteFileReference(
        id: String,
        referenceType: FileReferenceType,
        referrerId: String,
    ) {
        val reference = references.findById(id) ?: return
        if (reference.referenceType != referenceType || reference.referrerId != referrerId) {
            throw NotFoundException("파일 참조를 찾을 수 없습니다.")
        }
        uploads.findForUpdateById(reference.fileUploadId) ?: return
        if (references.deleteByIdAndRefTypeAndReferrer(id, referenceType, referrerId)) deletions.deleteFileUpload(reference.fileUploadId)
    }
}
