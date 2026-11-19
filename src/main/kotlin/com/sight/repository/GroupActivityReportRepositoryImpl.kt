package com.sight.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.QFileReference
import com.sight.domain.file.QFileUpload
import com.sight.domain.group.GroupActivityReport
import com.sight.domain.group.QGroupActivityReport
import com.sight.domain.schedule.QSchedule
import com.sight.domain.schedule.ScheduleState
import com.sight.domain.seminar.QBigSeminar
import com.sight.repository.dto.GroupActivityReportView
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.stereotype.Repository

@Repository
class GroupActivityReportRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
    private val entityManager: EntityManager,
) : GroupActivityReportRepositoryCustom {
    private val report = QGroupActivityReport.groupActivityReport
    private val reference = QFileReference.fileReference
    private val upload = QFileUpload.fileUpload
    private val seminar = QBigSeminar.bigSeminar
    private val schedule = QSchedule.schedule

    override fun findLockedByGroupIdAndId(
        groupId: Long,
        id: String,
    ): GroupActivityReport? {
        val locked =
            queryFactory.selectFrom(report)
                .where(report.groupId.eq(groupId), report.id.eq(id))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne()
                ?: return null
        entityManager.refresh(locked, LockModeType.PESSIMISTIC_WRITE)
        return locked
    }

    override fun findWithFileAndSeminarByGroupId(groupId: Long): List<GroupActivityReportView> =
        queryFactory
            .select(report, upload.id, upload.fileKey, schedule.scheduledAt, seminar.isSummerSeason, seminar.isSpeakAfter)
            .from(report)
            .leftJoin(reference)
            .on(reference.referenceType.eq(FileReferenceType.GROUP_ACTIVITY_REPORT).and(reference.ownerId.eq(report.id)))
            .leftJoin(upload)
            .on(upload.id.eq(reference.fileUploadId), upload.deletedAt.isNull)
            .leftJoin(seminar)
            .on(seminar.id.eq(report.seminarId))
            .leftJoin(schedule)
            .on(schedule.id.eq(seminar.scheduleId), schedule.state.eq(ScheduleState.PUBLIC))
            .where(report.groupId.eq(groupId))
            .fetch()
            .map { row ->
                GroupActivityReportView(
                    report = requireNotNull(row.get(report)),
                    fileUploadId = row.get(upload.id),
                    fileKey = row.get(upload.fileKey),
                    seminarScheduledAt = row.get(schedule.scheduledAt),
                    seminarIsSummerSeason = row.get(seminar.isSummerSeason),
                    seminarIsSpeakAfter = row.get(seminar.isSpeakAfter),
                )
            }
}
