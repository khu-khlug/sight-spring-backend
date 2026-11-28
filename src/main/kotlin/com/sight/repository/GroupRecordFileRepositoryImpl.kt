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
    private val recordFile = QGroupRecordFile.groupRecordFile
    private val fileReference = QFileReference.fileReference
    private val fileUpload = QFileUpload.fileUpload

    override fun findByFileUploadIdOrderById(fileUploadId: String): List<GroupRecordFileView> =
        findFiles(
            fileReference.referenceType.`in`(FileReferenceType.GROUP_RECORD_ATTACHMENT, FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT),
            fileReference.fileUploadId.eq(fileUploadId),
        )

    override fun findByRefTypeAndReferrerOrderById(
        refType: FileReferenceType,
        referrerId: String,
    ): List<GroupRecordFileView> = findByRefTypeAndReferrerInOrderById(refType, listOf(referrerId))

    override fun findByRefTypeAndReferrerInOrderById(
        refType: FileReferenceType,
        referrerIds: Collection<String>,
    ): List<GroupRecordFileView> {
        if (referrerIds.isEmpty()) return emptyList()
        return findFiles(fileReference.referenceType.eq(refType), fileReference.referrerId.`in`(referrerIds))
    }

    private fun findFiles(vararg conditions: BooleanExpression): List<GroupRecordFileView> =
        queryFactory
            .select(recordFile, fileReference, fileUpload)
            .from(recordFile)
            .join(fileReference).on(fileReference.id.eq(recordFile.fileReferenceId))
            .join(fileUpload).on(fileUpload.id.eq(fileReference.fileUploadId))
            .where(*conditions)
            .orderBy(recordFile.id.asc())
            .fetch()
            .map { row ->
                GroupRecordFileView(
                    recordFile = requireNotNull(row.get(recordFile)),
                    fileReference = requireNotNull(row.get(fileReference)),
                    fileUpload = requireNotNull(row.get(fileUpload)),
                )
            }
}
