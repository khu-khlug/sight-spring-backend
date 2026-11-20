package com.sight.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.QFileReference
import com.sight.domain.file.QFileUpload
import com.sight.domain.group.QGroupActivityReport
import com.sight.repository.dto.GroupActivityReportFileView
import org.springframework.stereotype.Repository

@Repository
class GroupActivityReportRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : GroupActivityReportRepositoryCustom {
    private val report = QGroupActivityReport.groupActivityReport
    private val reference = QFileReference.fileReference
    private val upload = QFileUpload.fileUpload

    override fun findWithFilesByGroupId(groupId: Long): List<GroupActivityReportFileView> =
        queryFactory
            .select(report, reference.id, upload.id, upload.fileKey)
            .from(report)
            .leftJoin(reference)
            .on(reference.referenceType.eq(FileReferenceType.GROUP_ACTIVITY_REPORT).and(reference.ownerId.eq(report.id)))
            .leftJoin(upload)
            .on(upload.id.eq(reference.fileUploadId).and(upload.deletedAt.isNull))
            .where(report.groupId.eq(groupId))
            .fetch()
            .map { row ->
                GroupActivityReportFileView(
                    report = requireNotNull(row.get(report)),
                    referenceId = row.get(reference.id),
                    fileUploadId = row.get(upload.id),
                    fileKey = row.get(upload.fileKey),
                )
            }
}
