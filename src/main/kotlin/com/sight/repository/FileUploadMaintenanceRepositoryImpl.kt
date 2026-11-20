package com.sight.repository

import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.file.QFileReference
import com.sight.domain.file.QFileUpload
import com.sight.repository.dto.FileUploadReservationView
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class FileUploadMaintenanceRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : FileUploadMaintenanceRepository {
    private val upload = QFileUpload.fileUpload
    private val reference = QFileReference.fileReference

    override fun findPendingReservationsAfter(
        cursor: String,
        horizon: Instant,
        limit: Int,
    ): List<FileUploadReservationView> =
        queryFactory
            .select(upload.id, upload.uploadCheckAt)
            .from(upload)
            .where(
                upload.id.gt(cursor),
                upload.isUploaded.isFalse,
                upload.deletedAt.isNull,
                upload.uploadCheckedAt.isNull,
                upload.uploadCheckAt.loe(horizon),
            )
            .orderBy(upload.id.asc())
            .limit(limit.toLong())
            .fetch()
            .map { row ->
                FileUploadReservationView(requireNotNull(row.get(upload.id)), requireNotNull(row.get(upload.uploadCheckAt)))
            }

    override fun findCleanupCandidatesAfter(
        cursor: String,
        at: Instant,
        limit: Int,
    ): List<String> =
        queryFactory
            .select(upload.id)
            .from(upload)
            .where(
                upload.id.gt(cursor),
                upload.deletedAt.isNotNull.or(
                    upload.uploadCheckAt.loe(at).and(
                        JPAExpressions.selectOne().from(reference).where(reference.fileUploadId.eq(upload.id)).notExists(),
                    ),
                ),
            )
            .orderBy(upload.id.asc())
            .limit(limit.toLong())
            .fetch()
}
