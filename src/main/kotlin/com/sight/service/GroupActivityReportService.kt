package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.InternalServerErrorException
import com.sight.core.exception.NotFoundException
import com.sight.domain.file.FileReferenceType
import com.sight.domain.group.GroupActivityReport
import com.sight.domain.group.GroupFile
import com.sight.domain.notification.NotificationCategory
import com.sight.domain.seminar.BigSeminar
import com.sight.repository.BigSeminarRepository
import com.sight.repository.GroupActivityReportRepository
import com.sight.repository.GroupFileRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.ScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class ActivityReportResult(
    val id: String,
    val groupId: Long,
    val seminarId: String,
    val isPresentation: Boolean,
    val reportFileKey: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class ActivityReportListItem(
    val id: String,
    val groupId: Long,
    val seminarId: String,
    val seminarDate: LocalDateTime?,
    val seminarIsSummerSeason: Boolean?,
    val seminarIsSpeakAfter: Boolean?,
    val isPresentation: Boolean,
    val reportFileUrl: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class ActivityReportListResult(
    val reports: List<ActivityReportListItem>,
)

// 활동보고 API는 응답 형식(파일 key, LocalDateTime 시각)을 유지한다.
// 파일 key는 활동보고의 공통 파일 참조를 통해 업로드 기록에서 조회한다.
@Service
class GroupActivityReportService(
    private val integerIdService: IntegerIdService,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupActivityReportRepository: GroupActivityReportRepository,
    private val bigSeminarRepository: BigSeminarRepository,
    private val scheduleRepository: ScheduleRepository,
    private val storageService: StorageService,
    private val pointService: PointService,
    private val notificationService: NotificationService,
    private val fileUploadService: FileUploadService,
    private val groupFileRepository: GroupFileRepository,
    private val fileReferences: FileReferenceService,
) {
    @Transactional
    fun getUploadLink(
        groupId: Long,
        requesterId: Long,
        requestPath: String,
    ): UploadLinkResult {
        groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }

        val uploadLink = fileUploadService.createUploadLink(requesterId, requestPath)
        groupFileRepository.save(GroupFile(id = integerIdService.nextId(), groupId = groupId, fileUploadId = uploadLink.fileUploadId))
        return uploadLink
    }

    @Transactional
    fun submitReport(
        groupId: Long,
        requesterId: Long,
        isPresentation: Boolean,
        fileUploadId: String,
        uploadLinkRequestPath: String,
    ): ActivityReportResult {
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }

        val seminar = findNextSeminar() ?: throw BadRequestException("활동보고 기간이 아닙니다.")
        if (groupActivityReportRepository.existsByGroupIdAndSeminarId(groupId, seminar.id)) {
            throw BadRequestException("이미 활동보고를 제출했습니다.")
        }
        val fileUpload = fileUploadService.validateFileUpload(fileUploadId, requesterId, uploadLinkRequestPath)
        val reportId = UlidCreator.getUlid().toString()

        val report =
            groupActivityReportRepository.save(
                GroupActivityReport(
                    id = reportId,
                    groupId = groupId,
                    seminarId = seminar.id,
                    isPresentation = isPresentation,
                    createdAt = Instant.now(),
                ),
            )

        fileReferences.createFileReference(fileUpload.id, FileReferenceType.GROUP_ACTIVITY_REPORT, reportId)
        fileUploadService.markAsUsed(fileUpload)

        val members = groupMemberRepository.findByGroupId(groupId)
        members.forEach { member ->
            pointService.givePoint(member.memberId, 150, "활동보고 제출")
            notificationService.createNotification(
                userId = member.memberId,
                category = NotificationCategory.GROUP,
                title = "활동보고 제출",
                content = "${group.title} 그룹의 활동보고가 제출되었습니다.",
            )
        }
        notificationService.createNotificationForManagers(
            category = NotificationCategory.GROUP,
            title = "활동보고 제출",
            content = "${group.title} 그룹의 활동보고가 제출되었습니다.",
        )

        return report.toResult(reportFileKey = fileUpload.fileKey)
    }

    @Transactional
    fun editReport(
        groupId: Long,
        reportId: String,
        requesterId: Long,
        isPresentation: Boolean?,
        fileUploadId: String?,
        uploadLinkRequestPath: String,
    ): ActivityReportResult {
        if (isPresentation == null && fileUploadId == null) throw BadRequestException("수정할 내용이 없습니다.")

        val report =
            groupActivityReportRepository.findLockedById(reportId) ?: throw NotFoundException("활동보고를 찾을 수 없습니다.")
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        if (report.groupId != groupId) throw NotFoundException("활동보고를 찾을 수 없습니다.")

        validateGroupActivitySubmitPeriod(report.seminarId)

        val oldFileUpload = getReportFileUpload(report)
        val newFileUpload =
            fileUploadId?.let {
                fileUploadService.validateFileUpload(it, requesterId, uploadLinkRequestPath)
            }
        val isFileChanged = newFileUpload != null && newFileUpload.id != oldFileUpload.id
        if (isFileChanged) {
            fileReferences.replaceSingleFileReference(requireNotNull(newFileUpload).id, FileReferenceType.GROUP_ACTIVITY_REPORT, report.id)
            fileUploadService.markAsUsed(newFileUpload)
        }

        val nextIsPresentation = isPresentation ?: report.isPresentation
        if (nextIsPresentation != report.isPresentation || isFileChanged) {
            report.update(isPresentation = nextIsPresentation, updatedAt = Instant.now())
        }
        val savedReport = groupActivityReportRepository.save(report)

        val members = groupMemberRepository.findByGroupId(groupId)
        val notificationContent = buildEditNotificationContent(isPresentation, fileUploadId, group.title)
        members.forEach { member ->
            notificationService.createNotification(
                userId = member.memberId,
                category = NotificationCategory.GROUP,
                title = "활동보고 수정",
                content = notificationContent,
            )
        }
        notificationService.createNotificationForManagers(
            category = NotificationCategory.GROUP,
            title = "활동보고 수정",
            content = notificationContent,
        )

        return savedReport.toResult(reportFileKey = (newFileUpload ?: oldFileUpload).fileKey)
    }

    @Transactional
    fun cancelReport(
        groupId: Long,
        reportId: String,
    ) {
        val report =
            groupActivityReportRepository.findLockedById(reportId) ?: throw NotFoundException("활동보고를 찾을 수 없습니다.")
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        if (report.groupId != groupId) throw NotFoundException("활동보고를 찾을 수 없습니다.")

        validateGroupActivitySubmitPeriod(report.seminarId)

        groupActivityReportRepository.delete(report)
        fileReferences.deleteSingleFileReferenceByOwner(FileReferenceType.GROUP_ACTIVITY_REPORT, report.id)

        val members = groupMemberRepository.findByGroupId(groupId)
        members.forEach { member ->
            pointService.givePoint(member.memberId, -150, "활동보고 취소")
            notificationService.createNotification(
                userId = member.memberId,
                category = NotificationCategory.GROUP,
                title = "활동보고 취소",
                content = "${group.title} 그룹의 활동보고가 취소되었습니다.",
            )
        }
        notificationService.createNotificationForManagers(
            category = NotificationCategory.GROUP,
            title = "활동보고 취소",
            content = "${group.title} 그룹의 활동보고가 취소되었습니다.",
        )
    }

    @Transactional(readOnly = true)
    fun listReports(groupId: Long): ActivityReportListResult {
        groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        val rows = groupActivityReportRepository.findWithFilesByGroupId(groupId)
        if (rows.groupBy { it.report.id }.values.any { it.size > 1 }) {
            throw InternalServerErrorException("활동보고 파일 참조가 중복되어 있습니다.")
        }
        val reports = rows.map { it.report }

        val seminars = bigSeminarRepository.findAllById(reports.map { it.seminarId }).associateBy { it.id }
        val schedules = scheduleRepository.findAllById(seminars.values.map { it.scheduleId }).associateBy { it.id }

        val items =
            rows.map { row ->
                val report = row.report
                val seminar = seminars[report.seminarId]
                val schedule = seminar?.let { schedules[it.scheduleId] }
                val fileKey = row.fileKey ?: throw NotFoundException("활동보고 파일을 찾을 수 없습니다.")

                ActivityReportListItem(
                    id = report.id,
                    groupId = report.groupId,
                    seminarId = report.seminarId,
                    seminarDate = schedule?.scheduledAt,
                    seminarIsSummerSeason = seminar?.isSummerSeason,
                    seminarIsSpeakAfter = seminar?.isSpeakAfter,
                    isPresentation = report.isPresentation,
                    reportFileUrl = storageService.getDownloadUrl(fileKey),
                    createdAt = report.createdAt.toUtcLocalDateTime(),
                    updatedAt = report.updatedAt.toUtcLocalDateTime(),
                )
            }

        return ActivityReportListResult(reports = items)
    }

    private fun getReportFileUpload(report: GroupActivityReport): com.sight.domain.file.FileUpload {
        val reference =
            fileReferences.getSingleFileReferenceByOwner(
                FileReferenceType.GROUP_ACTIVITY_REPORT,
                report.id,
            ) ?: throw NotFoundException("활동보고 파일 참조를 찾을 수 없습니다.")
        return fileUploadService.getFileUploadById(reference.fileUploadId)
    }

    private fun findNextSeminar(): BigSeminar? {
        val now = LocalDateTime.now()
        val upcomingSchedules = scheduleRepository.findByEndAtAfterOrderByScheduledAtAsc(now)
        return upcomingSchedules.firstNotNullOfOrNull { schedule ->
            bigSeminarRepository.findByScheduleId(schedule.id)
        }
    }

    private fun validateGroupActivitySubmitPeriod(seminarId: String) {
        val seminar =
            bigSeminarRepository.findById(seminarId)
                .orElseThrow { NotFoundException("세미나를 찾을 수 없습니다.") }
        val schedule =
            scheduleRepository.findById(seminar.scheduleId)
                .orElseThrow { NotFoundException("스케줄을 찾을 수 없습니다.") }
        if (schedule.endAt <= LocalDateTime.now()) throw BadRequestException("접수 기간이 지났습니다.")
    }

    private fun buildEditNotificationContent(
        isPresentation: Boolean?,
        fileUploadId: String?,
        groupTitle: String,
    ): String =
        when {
            isPresentation != null && fileUploadId != null ->
                "$groupTitle 그룹의 활동보고 발표 여부 및 제출 파일이 변경되었습니다."
            isPresentation != null -> "$groupTitle 그룹의 활동보고 발표 여부가 변경되었습니다."
            else -> "$groupTitle 그룹의 활동보고 제출 파일이 변경되었습니다."
        }

    private fun Instant.toUtcLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, ZoneOffset.UTC)

    private fun GroupActivityReport.toResult(reportFileKey: String): ActivityReportResult =
        ActivityReportResult(
            id = id,
            groupId = groupId,
            seminarId = seminarId,
            isPresentation = isPresentation,
            reportFileKey = reportFileKey,
            createdAt = createdAt.toUtcLocalDateTime(),
            updatedAt = updatedAt.toUtcLocalDateTime(),
        )
}
