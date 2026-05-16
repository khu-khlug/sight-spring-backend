package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.file.FileUpload
import com.sight.domain.group.Group
import com.sight.domain.group.GroupActivityReport
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupMember
import com.sight.domain.group.GroupState
import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.schedule.ScheduleState
import com.sight.domain.seminar.BigSeminar
import com.sight.repository.BigSeminarRepository
import com.sight.repository.FileUploadRepository
import com.sight.repository.GroupActivityReportRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.ScheduleRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GroupActivityReportServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupMemberRepository = mock<GroupMemberRepository>()
    private val fileUploadRepository = mock<FileUploadRepository>()
    private val groupActivityReportRepository = mock<GroupActivityReportRepository>()
    private val bigSeminarRepository = mock<BigSeminarRepository>()
    private val scheduleRepository = mock<ScheduleRepository>()
    private val storageService = mock<StorageService>()
    private val pointService = mock<PointService>()
    private val notificationService = mock<NotificationService>()
    private lateinit var service: GroupActivityReportService

    private val now = LocalDateTime.now()
    private val baseGroup =
        Group(
            id = 1L,
            category = GroupCategory.STUDY,
            title = "테스트 그룹",
            author = 10L,
            master = 10L,
            state = GroupState.PROGRESS,
        )
    private val baseSchedule =
        Schedule(
            id = 1L,
            category = ScheduleCategory.SEMINAR,
            title = "세미나",
            author = 1L,
            state = ScheduleState.PUBLIC,
            scheduledAt = now,
            endAt = now.plusDays(7),
        )
    private val baseSeminar =
        BigSeminar(
            id = "seminar-1",
            scheduleId = 1L,
            isSummerSeason = false,
            isSpeakAfter = false,
        )
    private val baseFileUpload =
        FileUpload(
            id = "file-1",
            fileKey = "key-1",
            memberId = 10L,
            apiPath = "/groups/1/activity-report/upload-link",
        )
    private val baseReport =
        GroupActivityReport(
            id = "report-1",
            groupId = 1L,
            seminarId = "seminar-1",
            reportFileKey = "key-1",
            isPresentation = false,
        )
    private val baseMembers = listOf(GroupMember(group = 1L, member = 20L), GroupMember(group = 1L, member = 30L))

    @BeforeEach
    fun setUp() {
        service =
            GroupActivityReportService(
                groupRepository,
                groupMemberRepository,
                fileUploadRepository,
                groupActivityReportRepository,
                bigSeminarRepository,
                scheduleRepository,
                storageService,
                pointService,
                notificationService,
            )
        given(storageService.isFileExists(any())).willReturn(true)
        given(storageService.generateUploadUrl(any())).willReturn("https://upload.example.com")
        given(storageService.getDownloadUrl(any())).willReturn("https://download.example.com")
        given(fileUploadRepository.save(any<FileUpload>())).willReturn(baseFileUpload)
        given(groupActivityReportRepository.save(any<GroupActivityReport>())).willReturn(baseReport)
    }

    // ── getUploadLink ──────────────────────────────────────────────────────────

    @Test
    fun `getUploadLink는 그룹장이 요청하면 url, fileKey, fileUploadId를 반환하고 file_upload를 저장한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))

        val result = service.getUploadLink(groupId = 1L, requesterId = 10L, requestPath = "/groups/1/activity-report/upload-link")

        assertEquals("https://upload.example.com", result.url)
        verify(fileUploadRepository).save(argThat { memberId == 10L && apiPath == "/groups/1/activity-report/upload-link" })
    }

    @Test
    fun `getUploadLink는 그룹원이 요청하면 403을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))

        assertFailsWith<ForbiddenException> {
            service.getUploadLink(groupId = 1L, requesterId = 99L, requestPath = "/groups/1/activity-report/upload-link")
        }
    }

    @Test
    fun `getUploadLink는 존재하지 않는 그룹에 요청하면 404를 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.empty())

        assertFailsWith<NotFoundException> {
            service.getUploadLink(groupId = 1L, requesterId = 10L, requestPath = "/groups/1/activity-report/upload-link")
        }
    }

    // ── submitReport ──────────────────────────────────────────────────────────

    @Test
    fun `submitReport는 그룹장이 접수 중인 세미나가 있을 때 보고서를 제출한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(scheduleRepository.findByEndAtAfterOrderByScheduledAtAsc(any())).willReturn(listOf(baseSchedule))
        given(bigSeminarRepository.findByScheduleId(1L)).willReturn(baseSeminar)
        given(groupActivityReportRepository.existsByGroupIdAndSeminarId(1L, "seminar-1")).willReturn(false)
        given(fileUploadRepository.findById("file-1")).willReturn(Optional.of(baseFileUpload))
        given(groupMemberRepository.findByGroupId(1L)).willReturn(baseMembers)

        val result =
            service.submitReport(
                groupId = 1L,
                requesterId = 10L,
                isPresentation = false,
                fileUploadId = "file-1",
                uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
            )

        assertEquals(false, result.isPresentation)
        assertEquals("seminar-1", result.seminarId)
    }

    @Test
    fun `submitReport는 접수 중인 세미나가 없으면 400을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(scheduleRepository.findByEndAtAfterOrderByScheduledAtAsc(any())).willReturn(emptyList())

        assertFailsWith<BadRequestException> {
            service.submitReport(
                groupId = 1L,
                requesterId = 10L,
                isPresentation = false,
                fileUploadId = "file-1",
                uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `submitReport는 이미 해당 세미나에 활동보고를 제출한 경우 400을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(scheduleRepository.findByEndAtAfterOrderByScheduledAtAsc(any())).willReturn(listOf(baseSchedule))
        given(bigSeminarRepository.findByScheduleId(1L)).willReturn(baseSeminar)
        given(groupActivityReportRepository.existsByGroupIdAndSeminarId(1L, "seminar-1")).willReturn(true)

        assertFailsWith<BadRequestException> {
            service.submitReport(
                groupId = 1L,
                requesterId = 10L,
                isPresentation = false,
                fileUploadId = "file-1",
                uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `submitReport는 그룹원이 요청하면 403을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))

        assertFailsWith<ForbiddenException> {
            service.submitReport(
                groupId = 1L,
                requesterId = 99L,
                isPresentation = false,
                fileUploadId = "file-1",
                uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `submitReport는 존재하지 않는 그룹에 요청하면 404를 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.empty())

        assertFailsWith<NotFoundException> {
            service.submitReport(
                groupId = 1L,
                requesterId = 10L,
                isPresentation = false,
                fileUploadId = "file-1",
                uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `submitReport는 fileUploadId에 해당하는 row가 없으면 400을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(scheduleRepository.findByEndAtAfterOrderByScheduledAtAsc(any())).willReturn(listOf(baseSchedule))
        given(bigSeminarRepository.findByScheduleId(1L)).willReturn(baseSeminar)
        given(groupActivityReportRepository.existsByGroupIdAndSeminarId(1L, "seminar-1")).willReturn(false)
        given(fileUploadRepository.findById("file-1")).willReturn(Optional.empty())

        assertFailsWith<BadRequestException> {
            service.submitReport(
                groupId = 1L,
                requesterId = 10L,
                isPresentation = false,
                fileUploadId = "file-1",
                uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `submitReport는 apiPath가 불일치하면 400을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(scheduleRepository.findByEndAtAfterOrderByScheduledAtAsc(any())).willReturn(listOf(baseSchedule))
        given(bigSeminarRepository.findByScheduleId(1L)).willReturn(baseSeminar)
        given(groupActivityReportRepository.existsByGroupIdAndSeminarId(1L, "seminar-1")).willReturn(false)
        given(fileUploadRepository.findById("file-1")).willReturn(
            Optional.of(baseFileUpload.copy(apiPath = "/other/path")),
        )

        assertFailsWith<BadRequestException> {
            service.submitReport(
                groupId = 1L,
                requesterId = 10L,
                isPresentation = false,
                fileUploadId = "file-1",
                uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `submitReport는 fileUpload의 memberId가 불일치하면 400을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(scheduleRepository.findByEndAtAfterOrderByScheduledAtAsc(any())).willReturn(listOf(baseSchedule))
        given(bigSeminarRepository.findByScheduleId(1L)).willReturn(baseSeminar)
        given(groupActivityReportRepository.existsByGroupIdAndSeminarId(1L, "seminar-1")).willReturn(false)
        given(fileUploadRepository.findById("file-1")).willReturn(
            Optional.of(baseFileUpload.copy(memberId = 99L)),
        )

        assertFailsWith<BadRequestException> {
            service.submitReport(
                groupId = 1L,
                requesterId = 10L,
                isPresentation = false,
                fileUploadId = "file-1",
                uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `submitReport는 isVerified가 true이면 400을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(scheduleRepository.findByEndAtAfterOrderByScheduledAtAsc(any())).willReturn(listOf(baseSchedule))
        given(bigSeminarRepository.findByScheduleId(1L)).willReturn(baseSeminar)
        given(groupActivityReportRepository.existsByGroupIdAndSeminarId(1L, "seminar-1")).willReturn(false)
        given(fileUploadRepository.findById("file-1")).willReturn(
            Optional.of(baseFileUpload.copy(isVerified = true)),
        )

        assertFailsWith<BadRequestException> {
            service.submitReport(
                groupId = 1L,
                requesterId = 10L,
                isPresentation = false,
                fileUploadId = "file-1",
                uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `submitReport는 제출 성공 시 그룹원 전원에게 ExPoint +50을 지급한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(scheduleRepository.findByEndAtAfterOrderByScheduledAtAsc(any())).willReturn(listOf(baseSchedule))
        given(bigSeminarRepository.findByScheduleId(1L)).willReturn(baseSeminar)
        given(groupActivityReportRepository.existsByGroupIdAndSeminarId(1L, "seminar-1")).willReturn(false)
        given(fileUploadRepository.findById("file-1")).willReturn(Optional.of(baseFileUpload))
        given(groupMemberRepository.findByGroupId(1L)).willReturn(baseMembers)

        service.submitReport(
            groupId = 1L,
            requesterId = 10L,
            isPresentation = false,
            fileUploadId = "file-1",
            uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
        )

        verify(pointService).givePoint(20L, 50, "활동보고 제출")
        verify(pointService).givePoint(30L, 50, "활동보고 제출")
    }

    // ── editReport ────────────────────────────────────────────────────────────

    @Test
    fun `editReport는 isPresentation과 fileUploadId가 모두 null이면 400을 반환한다`() {
        assertFailsWith<BadRequestException> {
            service.editReport(
                groupId = 1L,
                reportId = "report-1",
                requesterId = 10L,
                isPresentation = null,
                fileUploadId = null,
                uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `editReport는 그룹원이 요청하면 403을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))

        assertFailsWith<ForbiddenException> {
            service.editReport(
                groupId = 1L,
                reportId = "report-1",
                requesterId = 99L,
                isPresentation = true,
                fileUploadId = null,
                uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `editReport는 존재하지 않는 reportId 요청 시 404를 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findById("report-1")).willReturn(Optional.empty())

        assertFailsWith<NotFoundException> {
            service.editReport(
                groupId = 1L,
                reportId = "report-1",
                requesterId = 10L,
                isPresentation = true,
                fileUploadId = null,
                uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `editReport는 다른 그룹의 reportId를 전달하면 404를 반환한다`() {
        val otherGroupReport = baseReport.copy(groupId = 99L)
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findById("report-1")).willReturn(Optional.of(otherGroupReport))

        assertFailsWith<NotFoundException> {
            service.editReport(
                groupId = 1L,
                reportId = "report-1",
                requesterId = 10L,
                isPresentation = true,
                fileUploadId = null,
                uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `editReport는 접수 기간이 지난 세미나의 보고서는 400을 반환한다`() {
        val expiredSchedule = baseSchedule.copy(endAt = now.minusDays(1))
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findById("report-1")).willReturn(Optional.of(baseReport))
        given(bigSeminarRepository.findById("seminar-1")).willReturn(Optional.of(baseSeminar))
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(expiredSchedule))

        assertFailsWith<BadRequestException> {
            service.editReport(
                groupId = 1L,
                reportId = "report-1",
                requesterId = 10L,
                isPresentation = true,
                fileUploadId = null,
                uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `editReport는 isPresentation만 변경하면 isPresentation을 업데이트한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findById("report-1")).willReturn(Optional.of(baseReport))
        given(bigSeminarRepository.findById("seminar-1")).willReturn(Optional.of(baseSeminar))
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(baseSchedule))
        given(groupMemberRepository.findByGroupId(1L)).willReturn(baseMembers)

        service.editReport(
            groupId = 1L,
            reportId = "report-1",
            requesterId = 10L,
            isPresentation = true,
            fileUploadId = null,
            uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
        )

        verify(groupActivityReportRepository).save(argThat { isPresentation && reportFileKey == "key-1" })
        verify(storageService, never()).deleteFile(any())
    }

    // ── cancelReport ──────────────────────────────────────────────────────────

    @Test
    fun `cancelReport는 그룹장이 요청하면 보고서를 삭제한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findById("report-1")).willReturn(Optional.of(baseReport))
        given(bigSeminarRepository.findById("seminar-1")).willReturn(Optional.of(baseSeminar))
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(baseSchedule))
        given(groupMemberRepository.findByGroupId(1L)).willReturn(baseMembers)

        service.cancelReport(groupId = 1L, reportId = "report-1", requesterId = 10L, isManager = false)

        verify(groupActivityReportRepository).delete(baseReport)
        verify(fileUploadRepository).deleteByFileKey("key-1")
    }

    @Test
    fun `cancelReport는 운영진이 요청하면 보고서를 삭제한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findById("report-1")).willReturn(Optional.of(baseReport))
        given(bigSeminarRepository.findById("seminar-1")).willReturn(Optional.of(baseSeminar))
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(baseSchedule))
        given(groupMemberRepository.findByGroupId(1L)).willReturn(baseMembers)

        service.cancelReport(groupId = 1L, reportId = "report-1", requesterId = 99L, isManager = true)

        verify(groupActivityReportRepository).delete(baseReport)
    }

    @Test
    fun `cancelReport는 그룹원이 요청하면 403을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))

        assertFailsWith<ForbiddenException> {
            service.cancelReport(groupId = 1L, reportId = "report-1", requesterId = 99L, isManager = false)
        }
    }

    @Test
    fun `cancelReport는 다른 그룹의 reportId를 전달하면 404를 반환한다`() {
        val otherGroupReport = baseReport.copy(groupId = 99L)
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findById("report-1")).willReturn(Optional.of(otherGroupReport))

        assertFailsWith<NotFoundException> {
            service.cancelReport(groupId = 1L, reportId = "report-1", requesterId = 10L, isManager = false)
        }
    }

    @Test
    fun `cancelReport는 접수 기간이 지난 세미나의 보고서는 400을 반환한다`() {
        val expiredSchedule = baseSchedule.copy(endAt = now.minusDays(1))
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findById("report-1")).willReturn(Optional.of(baseReport))
        given(bigSeminarRepository.findById("seminar-1")).willReturn(Optional.of(baseSeminar))
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(expiredSchedule))

        assertFailsWith<BadRequestException> {
            service.cancelReport(groupId = 1L, reportId = "report-1", requesterId = 10L, isManager = false)
        }
    }

    @Test
    fun `cancelReport는 취소 성공 시 그룹원 전원에게 ExPoint -50을 지급한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findById("report-1")).willReturn(Optional.of(baseReport))
        given(bigSeminarRepository.findById("seminar-1")).willReturn(Optional.of(baseSeminar))
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(baseSchedule))
        given(groupMemberRepository.findByGroupId(1L)).willReturn(baseMembers)

        service.cancelReport(groupId = 1L, reportId = "report-1", requesterId = 10L, isManager = false)

        verify(pointService).givePoint(20L, -50, "활동보고 취소")
        verify(pointService).givePoint(30L, -50, "활동보고 취소")
    }

    // ── listReports ───────────────────────────────────────────────────────────

    @Test
    fun `listReports는 그룹원이 요청하면 활동보고 목록을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupMemberRepository.existsByGroupIdAndMemberId(1L, 20L)).willReturn(true)
        given(groupActivityReportRepository.findByGroupId(1L)).willReturn(listOf(baseReport))
        given(bigSeminarRepository.findById("seminar-1")).willReturn(Optional.of(baseSeminar))
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(baseSchedule))

        val result = service.listReports(groupId = 1L, requesterId = 20L, isManager = false)

        assertEquals(1, result.reports.size)
    }

    @Test
    fun `listReports는 운영진이 요청하면 활동보고 목록을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findByGroupId(1L)).willReturn(listOf(baseReport))
        given(bigSeminarRepository.findById("seminar-1")).willReturn(Optional.of(baseSeminar))
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(baseSchedule))

        val result = service.listReports(groupId = 1L, requesterId = 99L, isManager = true)

        assertEquals(1, result.reports.size)
    }

    @Test
    fun `listReports는 그룹 멤버가 아닌 사용자가 요청하면 403을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupMemberRepository.existsByGroupIdAndMemberId(1L, 99L)).willReturn(false)

        assertFailsWith<ForbiddenException> {
            service.listReports(groupId = 1L, requesterId = 99L, isManager = false)
        }
    }

    @Test
    fun `listReports는 존재하지 않는 그룹에 요청하면 404를 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.empty())

        assertFailsWith<NotFoundException> {
            service.listReports(groupId = 1L, requesterId = 10L, isManager = false)
        }
    }
}
