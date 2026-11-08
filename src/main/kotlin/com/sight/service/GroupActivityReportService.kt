package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
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
// 새 도메인은 파일을 업로드 기록 아이디로 참조하므로, 파일 key는 각 함수에서 업로드 기록을 조회해 채운다.
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

        val report =
            groupActivityReportRepository.save(
                GroupActivityReport(
                    id = UlidCreator.getUlid().toString(),
                    groupId = groupId,
                    seminarId = seminar.id,
                    isPresentation = isPresentation,
                    reportFileUploadId = fileUpload.id,
                    createdAt = Instant.now(),
                ),
            )

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

        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }

        val report =
            groupActivityReportRepository.findById(reportId).orElseThrow { NotFoundException("활동보고를 찾을 수 없습니다.") }
        if (report.groupId != groupId) throw NotFoundException("활동보고를 찾을 수 없습니다.")

        validateGroupActivitySubmitPeriod(report.seminarId)

        val oldFileUpload = fileUploadService.getFileUploadById(report.reportFileUploadId)
        val newFileUpload =
            fileUploadId?.let {
                fileUploadService.validateFileUpload(it, requesterId, uploadLinkRequestPath)
            }

        report.change(
            isPresentation = isPresentation,
            reportFileUploadId = newFileUpload?.id,
            at = Instant.now(),
        )
        newFileUpload?.let { fileUploadService.markAsUsed(it) }
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

        if (newFileUpload != null) {
            groupFileRepository.deleteByFileUploadId(oldFileUpload.id)
            fileUploadService.deleteFileUpload(oldFileUpload.id)
            storageService.deleteFile(oldFileUpload.fileKey)
        }

        return savedReport.toResult(reportFileKey = (newFileUpload ?: oldFileUpload).fileKey)
    }

    @Transactional
    fun cancelReport(
        groupId: Long,
        reportId: String,
    ) {
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }

        val report =
            groupActivityReportRepository.findById(reportId).orElseThrow { NotFoundException("활동보고를 찾을 수 없습니다.") }
        if (report.groupId != groupId) throw NotFoundException("활동보고를 찾을 수 없습니다.")

        validateGroupActivitySubmitPeriod(report.seminarId)

        val fileUpload = fileUploadService.getFileUploadById(report.reportFileUploadId)
        groupFileRepository.deleteByFileUploadId(fileUpload.id)
        fileUploadService.deleteFileUpload(fileUpload.id)
        groupActivityReportRepository.delete(report)

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

        storageService.deleteFile(fileUpload.fileKey)
    }

    @Transactional(readOnly = true)
    fun listReports(groupId: Long): ActivityReportListResult {
        groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        val reports = groupActivityReportRepository.findByGroupId(groupId)

        val seminars = bigSeminarRepository.findAllById(reports.map { it.seminarId }).associateBy { it.id }
        val schedules = scheduleRepository.findAllById(seminars.values.map { it.scheduleId }).associateBy { it.id }

        val items =
            reports.map { report ->
                val seminar = seminars[report.seminarId]
                val schedule = seminar?.let { schedules[it.scheduleId] }
                val fileUpload = fileUploadService.getFileUploadById(report.reportFileUploadId)

                ActivityReportListItem(
                    id = report.id,
                    groupId = report.groupId,
                    seminarId = report.seminarId,
                    seminarDate = schedule?.scheduledAt,
                    seminarIsSummerSeason = seminar?.isSummerSeason,
                    seminarIsSpeakAfter = seminar?.isSpeakAfter,
                    isPresentation = report.isPresentation,
                    reportFileUrl = storageService.getDownloadUrl(fileUpload.fileKey),
                    createdAt = report.createdAt.toUtcLocalDateTime(),
                    updatedAt = report.updatedAt.toUtcLocalDateTime(),
                )
            }

        return ActivityReportListResult(reports = items)
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
