package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.file.FileReference
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.testFileUpload
import com.sight.domain.group.GroupActivityReport
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupStatus
import com.sight.domain.group.testGroup
import com.sight.domain.group.testGroupMember
import com.sight.repository.ActivityReportPointRepository
import com.sight.repository.GroupActivityReportRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.dto.GroupActivityReportView
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import java.time.Instant
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GroupActivityReportServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupMemberRepository = mock<GroupMemberRepository>()
    private val groupActivityReportRepository = mock<GroupActivityReportRepository>()
    private val storageService = mock<StorageService>()
    private val activityReportPointRepository = mock<ActivityReportPointRepository>()
    private val seminarService = mock<SeminarService>()
    private val activityReportUploadService = mock<ActivityReportUploadService>()
    private val groupLogService = mock<GroupLogService>()
    private val transactions = mock<PlatformTransactionManager>()
    private val notificationService = mock<NotificationService>()
    private val fileUploadService = mock<FileUploadService>()
    private val fileReferenceService = mock<FileReferenceService>()
    private lateinit var service: GroupActivityReportService

    private val scheduledAt = LocalDateTime.of(2026, 12, 5, 14, 0)
    private val baseGroup =
        testGroup(
            id = 1L,
            category = GroupCategory.STUDY,
            title = "테스트 그룹",
            leaderMemberId = 10L,
            status = GroupStatus.PROGRESS,
        )
    private val baseFileUpload =
        testFileUpload(id = "file-1", fileKey = "key-1", uploaderMemberId = 10L)

    private val baseReport = createReport()
    private val baseReference = FileReference("reference-1", "file-1", FileReferenceType.GROUP_ACTIVITY_REPORT, "report-1")

    private fun createReport() =
        GroupActivityReport(
            id = "report-1",
            groupId = 1L,
            seminarId = "seminar-1",
            isPresentation = false,
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
                storageService,
                activityReportPointRepository,
                notificationService,
                fileUploadService,
                fileReferenceService,
                seminarService,
                activityReportUploadService,
                groupLogService,
                transactions,
            )
        given(storageService.getDownloadUrl(any(), any())).willReturn(
            SignedFileUrl("https://download.example.com", Instant.parse("2026-12-05T00:00:00Z")),
        )
        given(activityReportUploadService.createUploadLink(1L, 10L, 1024L)).willReturn(
            UploadLinkResult(url = "https://upload.example.com", fileKey = "key-1", fileUploadId = "file-1"),
        )
        given(transactions.getTransaction(any())).willAnswer { SimpleTransactionStatus() }
        given(groupRepository.findLockedById(1L)).willReturn(baseGroup)
        given(activityReportUploadService.checkUploadedObject(any(), any(), any())).willReturn(ActivityReportUploadStatus.AVAILABLE)
        given(activityReportUploadService.confirmUpload(any(), any(), any(), any())).willReturn(baseFileUpload)
        given(seminarService.getOpenSeminar(any())).willReturn(
            OpenSeminarResult(
                id = "seminar-1",
                isSummerSeason = false,
                isSpeakAfter = false,
                scheduledAt = scheduledAt.atZone(SEMINAR_TIME_ZONE).toInstant(),
                endAt = scheduledAt.plusHours(4).atZone(SEMINAR_TIME_ZONE).toInstant(),
            ),
        )
        given(fileUploadService.getFileUploadById("file-1")).willReturn(baseFileUpload)
        given(fileReferenceService.createFileReference(any(), any(), any())).willReturn(baseReference)
        given(fileReferenceService.getSingleFileReferenceByReferrer(any(), any())).willReturn(baseReference)
        given(fileReferenceService.replaceSingleFileReference(any(), any(), any())).willReturn(baseReference)
        given(groupActivityReportRepository.save(any<GroupActivityReport>())).willReturn(baseReport)
    }

    // ── getUploadLink ──────────────────────────────────────────────────────────

    @Test
    fun `getUploadLink는 활동보고 파일 발급을 위임하고 결과를 반환한다`() {
        val result = service.getUploadLink(groupId = 1L, requesterId = 10L, size = 1024L)

        assertEquals("https://upload.example.com", result.url)
        verify(activityReportUploadService).createUploadLink(1L, 10L, 1024L)
    }

    @Test
    fun `getUploadLink는 존재하지 않는 그룹에 요청하면 404를 반환한다`() {
        given(groupRepository.findLockedById(1L)).willReturn(null)

        assertFailsWith<NotFoundException> {
            service.getUploadLink(groupId = 1L, requesterId = 10L, size = 1024L)
        }
    }

    // ── submitReport ──────────────────────────────────────────────────────────

    @Test
    fun `submitReport는 그룹장이 접수 중인 세미나가 있을 때 보고서를 제출한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.existsByGroupIdAndSeminarId(1L, "seminar-1")).willReturn(false)
        given(groupMemberRepository.findByGroupId(1L)).willReturn(baseMembers)

        val result =
            service.submitReport(
                groupId = 1L,
                requesterId = 10L,
                isPresentation = false,
                fileUploadId = "file-1",
            )

        assertEquals(false, result.isPresentation)
        assertEquals("seminar-1", result.seminarId)
    }

    @Test
    fun `submitReport는 접수 중인 세미나가 없으면 400을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(seminarService.getOpenSeminar(any())).willReturn(null)

        assertFailsWith<BadRequestException> {
            service.submitReport(
                groupId = 1L,
                requesterId = 10L,
                isPresentation = false,
                fileUploadId = "file-1",
            )
        }
    }

    @Test
    fun `submitReport는 이미 해당 세미나에 활동보고를 제출한 경우 400을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.existsByGroupIdAndSeminarId(1L, "seminar-1")).willReturn(true)

        assertFailsWith<BadRequestException> {
            service.submitReport(
                groupId = 1L,
                requesterId = 10L,
                isPresentation = false,
                fileUploadId = "file-1",
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
            )
        }
    }

    @Test
    fun `submitReport는 파일 검증에 실패하면 예외를 전파한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.existsByGroupIdAndSeminarId(1L, "seminar-1")).willReturn(false)
        given(activityReportUploadService.checkUploadedObject(any(), any(), any())).willThrow(BadRequestException("파일을 찾을 수 없습니다."))

        assertFailsWith<BadRequestException> {
            service.submitReport(
                groupId = 1L,
                requesterId = 10L,
                isPresentation = false,
                fileUploadId = "file-1",
            )
        }
    }

    @Test
    fun `submitReport는 크기 초과 파일의 정리를 커밋하고 보고서는 만들지 않는다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        val uploadStatus = ActivityReportUploadStatus.TOO_LARGE
        given(activityReportUploadService.checkUploadedObject(1L, "file-1", 10L)).willReturn(uploadStatus)
        given(activityReportUploadService.confirmUpload(1L, "file-1", 10L, uploadStatus)).willReturn(null)

        val error =
            assertFailsWith<BadRequestException> {
                service.submitReport(groupId = 1L, requesterId = 10L, isPresentation = false, fileUploadId = "file-1")
            }

        assertEquals(uploadStatus.rejectionMessage, error.message)
        verify(transactions).commit(any())
        verify(groupActivityReportRepository, never()).save(any<GroupActivityReport>())
    }

    @Test
    fun `submitReport는 제출 성공 시 그룹원 전원에게 ExPoint +150을 지급한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.existsByGroupIdAndSeminarId(1L, "seminar-1")).willReturn(false)
        given(groupMemberRepository.findByGroupId(1L)).willReturn(baseMembers)

        service.submitReport(
            groupId = 1L,
            requesterId = 10L,
            isPresentation = false,
            fileUploadId = "file-1",
        )

        verify(activityReportPointRepository).addPoints(20L, 10L, 150, "활동보고 제출")
        verify(activityReportPointRepository).addPoints(30L, 10L, 150, "활동보고 제출")
    }

    // ── editReport ────────────────────────────────────────────────────────────

    @Test
    fun `editReport는 존재하지 않는 reportId 요청 시 404를 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findLockedByGroupIdAndId(1L, "report-1")).willReturn(null)

        assertFailsWith<NotFoundException> {
            service.editReport(
                groupId = 1L,
                reportId = "report-1",
                requesterId = 10L,
                isPresentation = true,
                fileUploadId = null,
            )
        }
    }

    @Test
    fun `editReport는 다른 그룹의 reportId를 전달하면 404를 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findLockedByGroupIdAndId(1L, "report-1")).willReturn(null)

        assertFailsWith<NotFoundException> {
            service.editReport(
                groupId = 1L,
                reportId = "report-1",
                requesterId = 10L,
                isPresentation = true,
                fileUploadId = null,
            )
        }
    }

    @Test
    fun `editReport는 접수 기간이 지난 세미나의 보고서는 400을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findLockedByGroupIdAndId(1L, "report-1")).willReturn(baseReport)
        doThrow(BadRequestException("접수 기간이 지났습니다.")).`when`(seminarService).requireActivityReportPeriod("seminar-1")

        assertFailsWith<BadRequestException> {
            service.editReport(
                groupId = 1L,
                reportId = "report-1",
                requesterId = 10L,
                isPresentation = true,
                fileUploadId = null,
            )
        }
    }

    @Test
    fun `editReport는 isPresentation만 변경하면 isPresentation을 업데이트한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findLockedByGroupIdAndId(1L, "report-1")).willReturn(baseReport)
        given(groupMemberRepository.findByGroupId(1L)).willReturn(baseMembers)

        service.editReport(
            groupId = 1L,
            reportId = "report-1",
            requesterId = 10L,
            isPresentation = true,
            fileUploadId = null,
        )

        assertEquals(true, baseReport.isPresentation)
        verify(storageService, never()).deleteFile(any())
    }

    // ── cancelReport ──────────────────────────────────────────────────────────

    @Test
    fun `cancelReport는 접수 기간 안의 활동보고를 삭제한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findLockedByGroupIdAndId(1L, "report-1")).willReturn(baseReport)
        given(groupMemberRepository.findByGroupId(1L)).willReturn(baseMembers)

        service.cancelReport(groupId = 1L, reportId = "report-1", requesterId = 10L)

        verify(groupActivityReportRepository).delete(baseReport)
        verify(fileReferenceService).deleteSingleFileReferenceByReferrer(FileReferenceType.GROUP_ACTIVITY_REPORT, "report-1")
        verify(storageService, never()).deleteFile(any())
    }

    @Test
    fun `cancelReport는 다른 그룹의 reportId를 전달하면 404를 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findLockedByGroupIdAndId(1L, "report-1")).willReturn(null)

        assertFailsWith<NotFoundException> {
            service.cancelReport(groupId = 1L, reportId = "report-1", requesterId = 10L)
        }
    }

    @Test
    fun `cancelReport는 접수 기간이 지난 세미나의 보고서는 400을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findLockedByGroupIdAndId(1L, "report-1")).willReturn(baseReport)
        doThrow(BadRequestException("접수 기간이 지났습니다.")).`when`(seminarService).requireActivityReportPeriod("seminar-1")

        assertFailsWith<BadRequestException> {
            service.cancelReport(groupId = 1L, reportId = "report-1", requesterId = 10L)
        }
    }

    @Test
    fun `cancelReport는 취소 성공 시 그룹원 전원에게 ExPoint -150을 지급한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findLockedByGroupIdAndId(1L, "report-1")).willReturn(baseReport)
        given(groupMemberRepository.findByGroupId(1L)).willReturn(baseMembers)

        service.cancelReport(groupId = 1L, reportId = "report-1", requesterId = 10L)

        verify(activityReportPointRepository).addPoints(20L, 10L, -150, "활동보고 취소")
        verify(activityReportPointRepository).addPoints(30L, 10L, -150, "활동보고 취소")
    }

    // ── listReports ───────────────────────────────────────────────────────────

    @Test
    fun `listReports는 그룹의 활동보고 목록을 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupActivityReportRepository.findWithFileAndSeminarByGroupId(1L)).willReturn(
            listOf(GroupActivityReportView(baseReport, "file-1", "key-1", scheduledAt, false, false)),
        )

        val result = service.listReports(groupId = 1L)

        val report = result.reports.single()
        assertEquals(scheduledAt.atZone(SEMINAR_TIME_ZONE).toInstant(), report.seminarDate)
        assertEquals("https://download.example.com", report.reportFileUrl)
        verify(storageService).getDownloadUrl("key-1", FileDownloadOptions("file-1", "application/octet-stream", inline = false))
    }

    @Test
    fun `listReports는 존재하지 않는 그룹에 요청하면 404를 반환한다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.empty())

        assertFailsWith<NotFoundException> {
            service.listReports(groupId = 1L)
        }
    }
}
