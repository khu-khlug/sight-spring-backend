package com.sight.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.QFileReference
import com.sight.domain.file.QFileUpload
import com.sight.domain.group.QGroupRecordFile
import com.sight.repository.dto.GroupRecordFileView
import org.springframework.stereotype.Repository

@Repository
class GroupRecordFileRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : GroupRecordFileRepositoryCustom {
    private val link = QGroupRecordFile.groupRecordFile
    private val reference = QFileReference.fileReference
    private val upload = QFileUpload.fileUpload

    override fun findByFileUploadIdOrderById(fileUploadId: String): List<GroupRecordFileView> =
        findFiles(
            reference.referenceType.`in`(FileReferenceType.GROUP_RECORD_ATTACHMENT, FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT),
            reference.fileUploadId.eq(fileUploadId),
        )

    override fun findByTypeAndOwnerOrderById(
        type: FileReferenceType,
        ownerId: String,
    ): List<GroupRecordFileView> = findByTypeAndOwnerInOrderById(type, listOf(ownerId))

    override fun findByTypeAndOwnerInOrderById(
        type: FileReferenceType,
        ownerIds: Collection<String>,
    ): List<GroupRecordFileView> {
        if (ownerIds.isEmpty()) return emptyList()
        return findFiles(reference.referenceType.eq(type), reference.ownerId.`in`(ownerIds))
    }

    private fun findFiles(vararg conditions: BooleanExpression): List<GroupRecordFileView> =
        queryFactory
            .select(link, reference, upload)
            .from(link)
            .join(reference).on(reference.id.eq(link.fileReferenceId))
            .join(upload).on(upload.id.eq(reference.fileUploadId))
            .where(*conditions)
            .orderBy(link.id.asc())
            .fetch()
            .map { row ->
                GroupRecordFileView(
                    link = requireNotNull(row.get(link)),
                    reference = requireNotNull(row.get(reference)),
                    upload = requireNotNull(row.get(upload)),
                )
            }
}
