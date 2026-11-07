package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.file.testFileUpload
import com.sight.domain.group.GroupActivityReport
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupFile
import com.sight.domain.group.GroupStatus
import com.sight.domain.group.testGroup
import com.sight.domain.group.testGroupMember
import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.schedule.ScheduleState
import com.sight.domain.seminar.BigSeminar
import com.sight.repository.BigSeminarRepository
import com.sight.repository.GroupActivityReportRepository
import com.sight.repository.GroupFileRepository
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
import java.time.Instant
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GroupActivityReportServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupMemberRepository = mock<GroupMemberRepository>()
    private val groupActivityReportRepository = mock<GroupActivityReportRepository>()
    private val bigSeminarRepository = mock<BigSeminarRepository>()
    private val scheduleRepository = mock<ScheduleRepository>()
    private val storageService = mock<StorageService>()
    private val pointService = mock<PointService>()
    private val notificationService = mock<NotificationService>()
    private val fileUploadService = mock<FileUploadService>()
    private val groupFileRepository = mock<GroupFileRepository>()
    private lateinit var service: GroupActivityReportService

    private val now = LocalDateTime.now()
    private val baseGroup =
        testGroup(
            id = 1L,
            category = GroupCategory.STUDY,
            title = "테스트 그룹",
            leaderMemberId = 10L,
            status = GroupStatus.PROGRESS,
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
        testFileUpload(id = "file-1", fileKey = "key-1", uploaderMemberId = 10L)

    private val baseReport = createReport()

    private fun createReport(groupId: Long = 1L) =
        GroupActivityReport(
            id = "report-1",
            groupId = groupId,
            seminarId = "seminar-1",
            isPresentation = false,
            reportFileUploadId = "file-1",
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        )

    private val baseMembers = listOf(testGroupMember(groupId = 1L, memberId = 20L), testGroupMember(groupId = 1L, memberId = 30L))

    @BeforeEach
    fun setUp() {
        service =
            GroupActivityReportService(
                groupRepository,
                groupMemberRepository,
                groupActivityReportRepository,
                bigSeminarRepository,
                scheduleRepository,
                storageService,
                pointService,
                notificationService,
                fileUploadService,
                groupFileRepository,
            )
        given(storageService.getDownloadUrl(any())).willReturn("https://download.example.com")
        given(fileUploadService.createUploadLink(any(), any())).willReturn(
            UploadLinkResult(url = "https://upload.example.com", fileKey = "key-1", fileUploadId = "file-1"),
        )
        given(fileUploadService.validateFileUpload(any(), any(), any())).willReturn(baseFileUpload)
        given(fileUploadService.getFileUploadById("file-1")).willReturn(baseFileUpload)
        given(groupActivityReportRepository.save(any<GroupActivityReport>())).willReturn(baseReport)
    }

    // ── getUploadLink ──────────────────────────────────────────────────────────

    @Test
    fun `getUploadLink는 그룹장이 요청하면 fileUploadService에 위임하고 결과를 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))

        val result = service.getUploadLink(groupId = 1L, requesterId = 10L, requestPath = "/groups/1/activity-report/upload-link")

        assertEquals("https://upload.example.com", result.url)
        verify(fileUploadService).createUploadLink(10L, "/groups/1/activity-report/upload-link")
        verify(groupFileRepository).save(argThat<GroupFile> { groupId == 1L && fileUploadId == "file-1" })
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
    fun `submitReport는 파일 검증에 실패하면 예외를 전파한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(scheduleRepository.findByEndAtAfterOrderByScheduledAtAsc(any())).willReturn(listOf(baseSchedule))
        given(bigSeminarRepository.findByScheduleId(1L)).willReturn(baseSeminar)
        given(groupActivityReportRepository.existsByGroupIdAndSeminarId(1L, "seminar-1")).willReturn(false)
        given(fileUploadService.validateFileUpload(any(), any(), any())).willThrow(BadRequestException("파일을 찾을 수 없습니다."))

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
    fun `submitReport는 제출 성공 시 그룹원 전원에게 ExPoint +150을 지급한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(scheduleRepository.findByEndAtAfterOrderByScheduledAtAsc(any())).willReturn(listOf(baseSchedule))
        given(bigSeminarRepository.findByScheduleId(1L)).willReturn(baseSeminar)
        given(groupActivityReportRepository.existsByGroupIdAndSeminarId(1L, "seminar-1")).willReturn(false)
        given(groupMemberRepository.findByGroupId(1L)).willReturn(baseMembers)

        service.submitReport(
            groupId = 1L,
            requesterId = 10L,
            isPresentation = false,
            fileUploadId = "file-1",
            uploadLinkRequestPath = "/groups/1/activity-report/upload-link",
        )

        verify(pointService).givePoint(20L, 150, "활동보고 제출")
        verify(pointService).givePoint(30L, 150, "활동보고 제출")
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
        val otherGroupReport = createReport(groupId = 99L)
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

        verify(groupActivityReportRepository).save(argThat { isPresentation && reportFileUploadId == "file-1" })
        verify(storageService, never()).deleteFile(any())
    }

    // ── cancelReport ──────────────────────────────────────────────────────────

    @Test
    fun `cancelReport는 접수 기간 안의 활동보고를 삭제한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findById("report-1")).willReturn(Optional.of(baseReport))
        given(bigSeminarRepository.findById("seminar-1")).willReturn(Optional.of(baseSeminar))
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(baseSchedule))
        given(groupMemberRepository.findByGroupId(1L)).willReturn(baseMembers)

        service.cancelReport(groupId = 1L, reportId = "report-1")

        verify(groupActivityReportRepository).delete(baseReport)
        verify(groupFileRepository).deleteByFileUploadId("file-1")
        verify(fileUploadService).deleteFileUpload("file-1")
        verify(storageService).deleteFile("key-1")
    }

    @Test
    fun `cancelReport는 다른 그룹의 reportId를 전달하면 404를 반환한다`() {
        val otherGroupReport = createReport(groupId = 99L)
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findById("report-1")).willReturn(Optional.of(otherGroupReport))

        assertFailsWith<NotFoundException> {
            service.cancelReport(groupId = 1L, reportId = "report-1")
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
            service.cancelReport(groupId = 1L, reportId = "report-1")
        }
    }

    @Test
    fun `cancelReport는 취소 성공 시 그룹원 전원에게 ExPoint -150을 지급한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findById("report-1")).willReturn(Optional.of(baseReport))
        given(bigSeminarRepository.findById("seminar-1")).willReturn(Optional.of(baseSeminar))
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(baseSchedule))
        given(groupMemberRepository.findByGroupId(1L)).willReturn(baseMembers)

        service.cancelReport(groupId = 1L, reportId = "report-1")

        verify(pointService).givePoint(20L, -150, "활동보고 취소")
        verify(pointService).givePoint(30L, -150, "활동보고 취소")
    }

    // ── listReports ───────────────────────────────────────────────────────────

    @Test
    fun `listReports는 그룹의 활동보고 목록을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findByGroupId(1L)).willReturn(listOf(baseReport))
        given(bigSeminarRepository.findById("seminar-1")).willReturn(Optional.of(baseSeminar))
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(baseSchedule))

        val result = service.listReports(groupId = 1L)

        assertEquals(1, result.reports.size)
    }

    @Test
    fun `listReports는 존재하지 않는 그룹에 요청하면 404를 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.empty())

        assertFailsWith<NotFoundException> {
            service.listReports(groupId = 1L)
        }
    }
}
