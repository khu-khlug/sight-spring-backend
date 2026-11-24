package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadGatewayException
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.InternalServerErrorException
import com.sight.core.exception.NotFoundException
import com.sight.domain.expoint.ExpointRule
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.FileUpload
import com.sight.domain.group.Group
import com.sight.domain.group.GroupActivityReport
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupLogExpointChange
import com.sight.domain.group.GroupLogType
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.GroupActivityReportRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

data class ActivityReportResult(
    val id: String,
    val groupId: Long,
    val seminarId: String,
    val isPresentation: Boolean,
    val reportFileKey: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ActivityReportListItem(
    val id: String,
    val groupId: Long,
    val seminarId: String,
    val seminarDate: Instant?,
    val seminarIsSummerSeason: Boolean?,
    val seminarIsSpeakAfter: Boolean?,
    val isPresentation: Boolean,
    val reportFileUrl: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ActivityReportListResult(
    val reports: List<ActivityReportListItem>,
)

@Service
class GroupActivityReportService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupActivityReportRepository: GroupActivityReportRepository,
    private val storageService: StorageService,
    private val pointService: PointService,
    private val notificationService: NotificationService,
    private val fileUploadService: FileUploadService,
    private val fileReferenceService: FileReferenceService,
    private val seminarService: SeminarService,
    private val activityReportUploadService: ActivityReportUploadService,
    private val groupLogService: GroupLogService,
    transactionManager: PlatformTransactionManager,
) {
    private val writeTransaction = TransactionTemplate(transactionManager)

    @Transactional
    fun getUploadLink(
        groupId: Long,
        requesterId: Long,
        size: Long,
    ): UploadLinkResult {
        requireActiveGroup(groupRepository.findLockedById(groupId))
        return activityReportUploadService.createUploadLink(groupId, requesterId, size)
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun submitReport(
        groupId: Long,
        requesterId: Long,
        isPresentation: Boolean,
        fileUploadId: String,
    ): ActivityReportResult {
        requireActiveGroup(groupRepository.findById(groupId).orElse(null))
        requireSubmissionSeminar(groupId)
        val uploadStatus = activityReportUploadService.checkUploadedObject(groupId, fileUploadId, requesterId)
        val result =
            writeTransaction.execute {
                val group = requireActiveGroup(groupRepository.findLockedById(groupId))
                val seminar = requireSubmissionSeminar(groupId)
                val file =
                    activityReportUploadService.confirmUpload(groupId, fileUploadId, requesterId, uploadStatus)
                        ?: return@execute null
                createReport(group, seminar.id, requesterId, isPresentation, file)
            }
        // 파일 삭제 표시가 커밋되어야 저장소 삭제가 시작된다.
        return result ?: throw BadRequestException(requireNotNull(uploadStatus.rejectionMessage))
    }

    private fun requireSubmissionSeminar(groupId: Long): OpenSeminarResult {
        val seminar = seminarService.getOpenSeminar(Instant.now()) ?: throw BadRequestException("활동보고 기간이 아닙니다.")
        if (groupActivityReportRepository.existsByGroupIdAndSeminarId(groupId, seminar.id)) {
            throw BadRequestException("이미 활동보고를 제출했습니다.")
        }
        return seminar
    }

    private fun createReport(
        group: Group,
        seminarId: String,
        requesterId: Long,
        isPresentation: Boolean,
        fileUpload: FileUpload,
    ): ActivityReportResult {
        val reportId = UlidCreator.getUlid().toString()
        val now = Instant.now()

        val report =
            groupActivityReportRepository.save(
                GroupActivityReport(
                    id = reportId,
                    groupId = group.id,
                    seminarId = seminarId,
                    isPresentation = isPresentation,
                    createdAt = now,
                ),
            )

        fileReferenceService.createFileReference(fileUpload.id, FileReferenceType.GROUP_ACTIVITY_REPORT, reportId)
        fileUploadService.markAsUsed(fileUpload)

        val memberIds = getMemberIds(group.id)
        val point = ExpointRule.보고서제출.point
        val expointChange =
            memberIds.takeIf { it.isNotEmpty() }
                ?.let { GroupLogExpointChange(targetId = report.id, point = point, memberIds = it) }
        recordReportActivity(group, requesterId, GroupLogData.ActivityReportSubmitted(report.id, seminarId), now, expointChange)
        notifyMembers(memberIds, "활동보고 제출", "${group.title} 그룹의 활동보고가 제출되었습니다.")
        memberIds.forEach { memberId ->
            pointService.givePoint(memberId, point, "활동보고 제출")
        }

        return report.toResult(reportFileKey = fileUpload.fileKey)
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun editReport(
        groupId: Long,
        reportId: String,
        requesterId: Long,
        isPresentation: Boolean?,
        fileUploadId: String?,
    ): ActivityReportResult {
        val uploadStatus =
            fileUploadId?.let {
                requireActiveGroup(groupRepository.findById(groupId).orElse(null))
                val report = requireReport(groupActivityReportRepository.findByGroupIdAndId(groupId, reportId))
                seminarService.requireActivityReportPeriod(report.seminarId)
                activityReportUploadService.checkUploadedObject(groupId, it, requesterId)
            }
        val result =
            writeTransaction.execute {
                val group = requireActiveGroup(groupRepository.findLockedById(groupId))
                val report = requireReport(groupActivityReportRepository.findLockedByGroupIdAndId(groupId, reportId))
                seminarService.requireActivityReportPeriod(report.seminarId)
                val newFile =
                    fileUploadId?.let {
                        activityReportUploadService.confirmUpload(groupId, it, requesterId, requireNotNull(uploadStatus))
                            ?: return@execute null
                    }
                updateReport(group, report, requesterId, isPresentation, newFile)
            }
        return result ?: throw BadRequestException(requireNotNull(uploadStatus?.rejectionMessage))
    }

    private fun updateReport(
        group: Group,
        report: GroupActivityReport,
        requesterId: Long,
        isPresentation: Boolean?,
        newFileUpload: FileUpload?,
    ): ActivityReportResult {
        val fileUpload = newFileUpload ?: getReportFileUpload(report)
        val isFileChanged = newFileUpload != null
        val nextIsPresentation = isPresentation ?: report.isPresentation
        val isPresentationChanged = nextIsPresentation != report.isPresentation
        if (!isPresentationChanged && !isFileChanged) return report.toResult(fileUpload.fileKey)
        if (newFileUpload != null) {
            fileReferenceService.replaceSingleFileReference(newFileUpload.id, FileReferenceType.GROUP_ACTIVITY_REPORT, report.id)
            fileUploadService.markAsUsed(newFileUpload)
        }

        val now = Instant.now()
        report.update(isPresentation = nextIsPresentation, updatedAt = now)
        recordReportActivity(group, requesterId, GroupLogData.ActivityReportUpdated(report.id, report.seminarId), now)
        notifyMembers(getMemberIds(group.id), "활동보고 수정", buildEditNotificationContent(isPresentationChanged, isFileChanged, group.title))
        return report.toResult(reportFileKey = fileUpload.fileKey)
    }

    @Transactional
    fun cancelReport(
        groupId: Long,
        reportId: String,
        requesterId: Long,
    ) {
        val group = requireActiveGroup(groupRepository.findLockedById(groupId))
        val report = requireReport(groupActivityReportRepository.findLockedByGroupIdAndId(groupId, reportId))

        seminarService.requireActivityReportPeriod(report.seminarId)

        groupActivityReportRepository.delete(report)
        fileReferenceService.deleteSingleFileReferenceByReferrer(FileReferenceType.GROUP_ACTIVITY_REPORT, report.id)

        val memberIds = getMemberIds(group.id)
        group.resumeByActivity()
        group.recordActivity(Instant.now())
        notifyMembers(memberIds, "활동보고 취소", "${group.title} 그룹의 활동보고가 취소되었습니다.")
        groupLogService.revertExpointChange(
            groupId = group.id,
            actorMemberId = requesterId,
            targetId = report.id,
            targetLogTypes = setOf(GroupLogType.ACTIVITY_REPORT_SUBMITTED),
            revertLog = GroupLogData.ActivityReportCancelled(report.id, report.seminarId),
            message = "활동보고 취소",
            fallbackRule = ExpointRule.보고서제출취소,
            fallbackMemberIds = memberIds,
        )
    }

    private fun notifyMembers(
        memberIds: List<Long>,
        title: String,
        content: String,
    ) {
        memberIds.forEach { memberId ->
            notificationService.createNotification(
                userId = memberId,
                category = NotificationCategory.GROUP,
                title = title,
                content = content,
            )
        }
        notificationService.createNotificationForManagers(
            category = NotificationCategory.GROUP,
            title = title,
            content = content,
        )
    }

    private fun getMemberIds(groupId: Long): List<Long> =
        groupMemberRepository.findByGroupId(groupId).map { it.memberId }.distinct().sorted()

    @Transactional(readOnly = true)
    fun listReports(groupId: Long): ActivityReportListResult {
        groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        val rows = groupActivityReportRepository.findWithFileAndSeminarByGroupId(groupId)
        if (rows.map { it.report.id }.toSet().size != rows.size) {
            throw InternalServerErrorException("활동보고 파일 참조가 중복되어 있습니다.")
        }
        val items =
            rows.map { row ->
                val report = row.report
                val fileKey = row.fileKey ?: throw NotFoundException("활동보고 파일을 찾을 수 없습니다.")
                val fileUploadId = row.fileUploadId ?: throw NotFoundException("활동보고 파일을 찾을 수 없습니다.")

                ActivityReportListItem(
                    id = report.id,
                    groupId = report.groupId,
                    seminarId = report.seminarId,
                    seminarDate = row.seminarScheduledAt?.atZone(SEMINAR_TIME_ZONE)?.toInstant(),
                    seminarIsSummerSeason = row.seminarIsSummerSeason,
                    seminarIsSpeakAfter = row.seminarIsSpeakAfter,
                    isPresentation = report.isPresentation,
                    reportFileUrl = getReportFileUrl(fileKey, fileUploadId),
                    createdAt = report.createdAt,
                    updatedAt = report.updatedAt,
                )
            }

        return ActivityReportListResult(reports = items)
    }

    private fun getReportFileUpload(report: GroupActivityReport): FileUpload {
        val reference =
            fileReferenceService.getSingleFileReferenceByReferrer(
                FileReferenceType.GROUP_ACTIVITY_REPORT,
                report.id,
            ) ?: throw NotFoundException("활동보고 파일 참조를 찾을 수 없습니다.")
        return fileUploadService.getFileUploadById(reference.fileUploadId)
    }

    private fun getReportFileUrl(
        key: String,
        fileUploadId: String,
    ): String =
        try {
            storageService.getDownloadUrl(
                key,
                FileDownloadOptions(fileName = fileUploadId, contentType = "application/octet-stream", inline = false),
            ).url
        } catch (_: StorageOperationException) {
            throw BadGatewayException("활동보고 파일 주소를 발급할 수 없습니다.")
        }

    private fun requireActiveGroup(group: Group?): Group {
        val existing = group ?: throw NotFoundException("그룹을 찾을 수 없습니다.")
        if (existing.status.isEnded) throw BadRequestException("종료된 그룹입니다.")
        return existing
    }

    private fun requireReport(report: GroupActivityReport?): GroupActivityReport = report ?: throw NotFoundException("활동보고를 찾을 수 없습니다.")

    private fun recordReportActivity(
        group: Group,
        actorId: Long,
        log: GroupLogData,
        at: Instant,
        expointChange: GroupLogExpointChange? = null,
    ) {
        group.resumeByActivity()
        group.recordActivity(at)
        groupLogService.createLog(group.id, actorId, log, expointChange)
    }

    private fun buildEditNotificationContent(
        isPresentationChanged: Boolean,
        isFileChanged: Boolean,
        groupTitle: String,
    ): String =
        when {
            isPresentationChanged && isFileChanged ->
                "$groupTitle 그룹의 활동보고 발표 여부 및 제출 파일이 변경되었습니다."
            isPresentationChanged -> "$groupTitle 그룹의 활동보고 발표 여부가 변경되었습니다."
            else -> "$groupTitle 그룹의 활동보고 제출 파일이 변경되었습니다."
        }

    private fun GroupActivityReport.toResult(reportFileKey: String): ActivityReportResult =
        ActivityReportResult(
            id = id,
            groupId = groupId,
            seminarId = seminarId,
            isPresentation = isPresentation,
            reportFileKey = reportFileKey,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
