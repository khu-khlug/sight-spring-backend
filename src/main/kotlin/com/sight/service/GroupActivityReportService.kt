package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupActivityReport
import com.sight.domain.notification.NotificationCategory
import com.sight.domain.seminar.BigSeminar
import com.sight.repository.BigSeminarRepository
import com.sight.repository.GroupActivityReportRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.ScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

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

@Service
class GroupActivityReportService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupActivityReportRepository: GroupActivityReportRepository,
    private val bigSeminarRepository: BigSeminarRepository,
    private val scheduleRepository: ScheduleRepository,
    private val storageService: StorageService,
    private val pointService: PointService,
    private val notificationService: NotificationService,
    private val fileUploadService: FileUploadService,
) {
    @Transactional
    fun getUploadLink(
        groupId: Long,
        requesterId: Long,
        requestPath: String,
    ): UploadLinkResult {
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        if (group.master != requesterId) throw ForbiddenException("그룹장만 업로드 링크를 발급할 수 있습니다.")

        return fileUploadService.createUploadLink(requesterId, requestPath)
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
        if (group.master != requesterId) throw ForbiddenException("그룹장만 활동보고를 제출할 수 있습니다.")

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
                    reportFileKey = fileUpload.fileKey,
                    isPresentation = isPresentation,
                ),
            )

        fileUploadService.markAsUsed(fileUpload)

        val members = groupMemberRepository.findByGroupId(groupId)
        members.forEach { member ->
            pointService.givePoint(member.member, 150, "활동보고 제출")
            notificationService.createNotification(
                userId = member.member,
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

        return report.toResult()
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
        if (group.master != requesterId) throw ForbiddenException("그룹장만 활동보고를 수정할 수 있습니다.")

        var report =
            groupActivityReportRepository.findById(reportId).orElseThrow { NotFoundException("활동보고를 찾을 수 없습니다.") }
        if (report.groupId != groupId) throw NotFoundException("활동보고를 찾을 수 없습니다.")

        validateGroupActivitySubmitPeriod(report.seminarId)

        var oldFileKey: String? = null

        if (fileUploadId != null) {
            val fileUpload = fileUploadService.validateFileUpload(fileUploadId, requesterId, uploadLinkRequestPath)
            oldFileKey = report.reportFileKey
            report = report.copy(reportFileKey = fileUpload.fileKey)
            fileUploadService.markAsUsed(fileUpload)
        }

        if (isPresentation != null) {
            report = report.copy(isPresentation = isPresentation)
        }

        val savedReport = groupActivityReportRepository.save(report)

        val members = groupMemberRepository.findByGroupId(groupId)
        val notificationContent = buildEditNotificationContent(isPresentation, fileUploadId, group.title)
        members.forEach { member ->
            notificationService.createNotification(
                userId = member.member,
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

        oldFileKey?.let { storageService.deleteFile(it) }

        return savedReport.toResult()
    }

    @Transactional
    fun cancelReport(
        groupId: Long,
        reportId: String,
        requesterId: Long,
        isManager: Boolean,
    ) {
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        if (!isManager && group.master != requesterId) throw ForbiddenException("그룹장 또는 운영진만 활동보고를 취소할 수 있습니다.")

        val report =
            groupActivityReportRepository.findById(reportId).orElseThrow { NotFoundException("활동보고를 찾을 수 없습니다.") }
        if (report.groupId != groupId) throw NotFoundException("활동보고를 찾을 수 없습니다.")

        validateGroupActivitySubmitPeriod(report.seminarId)

        fileUploadService.deleteByFileKey(report.reportFileKey)
        groupActivityReportRepository.delete(report)

        val members = groupMemberRepository.findByGroupId(groupId)
        members.forEach { member ->
            pointService.givePoint(member.member, -150, "활동보고 취소")
            notificationService.createNotification(
                userId = member.member,
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

        storageService.deleteFile(report.reportFileKey)
    }

    @Transactional(readOnly = true)
    fun listReports(
        groupId: Long,
        requesterId: Long,
        isManager: Boolean,
    ): ActivityReportListResult {
        groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        if (!isManager && !groupMemberRepository.existsByGroupIdAndMemberId(groupId, requesterId)) {
            throw ForbiddenException("그룹 멤버만 활동보고를 조회할 수 있습니다.")
        }

        val reports = groupActivityReportRepository.findByGroupId(groupId)

        val seminars = bigSeminarRepository.findAllById(reports.map { it.seminarId }).associateBy { it.id }
        val schedules = scheduleRepository.findAllById(seminars.values.map { it.scheduleId }).associateBy { it.id }

        val items =
            reports.map { report ->
                val seminar = seminars[report.seminarId]
                val schedule = seminar?.let { schedules[it.scheduleId] }

                ActivityReportListItem(
                    id = report.id,
                    groupId = report.groupId,
                    seminarDate = schedule?.scheduledAt,
                    seminarIsSummerSeason = seminar?.isSummerSeason,
                    seminarIsSpeakAfter = seminar?.isSpeakAfter,
                    isPresentation = report.isPresentation,
                    reportFileUrl = storageService.getDownloadUrl(report.reportFileKey),
                    createdAt = report.createdAt,
                    updatedAt = report.updatedAt,
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

    private fun GroupActivityReport.toResult(): ActivityReportResult =
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
