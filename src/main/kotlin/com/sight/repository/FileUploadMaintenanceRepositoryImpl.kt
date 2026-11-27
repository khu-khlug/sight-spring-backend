package com.sight.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.file.QFileUpload
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class FileUploadMaintenanceRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : FileUploadMaintenanceRepository {
    private val upload = QFileUpload.fileUpload

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
                upload.deletedAt.isNotNull.or(upload.uploadCheckAt.loe(at)),
            )
            .orderBy(upload.id.asc())
            .limit(limit.toLong())
            .fetch()
}
