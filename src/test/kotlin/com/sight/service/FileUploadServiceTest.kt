package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.file.FileUpload
import com.sight.domain.file.testFileUpload
import com.sight.repository.FileUploadRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.Instant
import kotlin.test.assertFailsWith

class FileUploadServiceTest {
    private val fileUploadRepository = mock<FileUploadRepository>()
    private val storageService = mock<StorageService>()
    private val deletions = mock<FileUploadDeletionService>()
    private lateinit var service: FileUploadService

    private val baseFileUpload = testFileUpload()

    @BeforeEach
    fun setUp() {
        service = FileUploadService(fileUploadRepository, storageService, deletions)
        given(storageService.generateUploadUrl(any(), any())).willReturn("https://upload.example.com")
        given(storageService.isFileExists(any())).willReturn(true)
        given(fileUploadRepository.saveAndFlush(any<FileUpload>())).willReturn(baseFileUpload)
    }

    // ── createUploadLink ──────────────────────────────────────────────────────

    @Test
    fun `createUploadLink는 FileUpload를 저장하고 url, fileKey, fileUploadId를 반환한다`() {
        val result =
            service.createUploadLink(
                memberId = 10L,
                apiPath = "/groups/1/activity-report/upload-link",
                size = FileUpload.MAX_FILE_SIZE,
            )

        verify(storageService).generateUploadUrl(result.fileKey, FileUpload.MAX_FILE_SIZE)
        verify(fileUploadRepository).saveAndFlush(
            argThat {
                uploaderMemberId == 10L && apiPath == "/groups/1/activity-report/upload-link" && size == FileUpload.MAX_FILE_SIZE
            },
        )
        assert(result.url == "https://upload.example.com")
        assert(result.fileUploadId == "file-1")
    }

    @Test
    fun `최대 크기를 초과한 파일에는 업로드 링크를 발급하지 않는다`() {
        assertThrows<BadRequestException> {
            service.createUploadLink(10L, "/groups/1/activity-report/upload-link", FileUpload.MAX_FILE_SIZE + 1)
        }

        verify(storageService, never()).generateUploadUrl(any(), any())
        verify(fileUploadRepository, never()).saveAndFlush(any<FileUpload>())
    }

    // ── validateFileUpload ────────────────────────────────────────────────────

    @Test
    fun `validateFileUpload는 유효한 파일이면 FileUpload를 반환한다`() {
        given(fileUploadRepository.findForUpdateById("file-1")).willReturn(baseFileUpload)

        val result =
            service.validateFileUpload(
                fileUploadId = "file-1",
                memberId = 10L,
                apiPath = "/groups/1/activity-report/upload-link",
            )

        assert(result.id == "file-1")
    }

    @Test
    fun `validateFileUpload는 fileUploadId에 해당하는 row가 없으면 404를 반환한다`() {
        given(fileUploadRepository.findForUpdateById("file-1")).willReturn(null)

        assertFailsWith<NotFoundException> {
            service.validateFileUpload(
                fileUploadId = "file-1",
                memberId = 10L,
                apiPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `validateFileUpload는 apiPath가 불일치하면 422를 반환한다`() {
        given(fileUploadRepository.findForUpdateById("file-1")).willReturn(
            testFileUpload(apiPath = "/other/path"),
        )

        assertFailsWith<UnprocessableEntityException> {
            service.validateFileUpload(
                fileUploadId = "file-1",
                memberId = 10L,
                apiPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `validateFileUpload는 발급자가 불일치하면 403을 반환한다`() {
        given(fileUploadRepository.findForUpdateById("file-1")).willReturn(
            testFileUpload(uploaderMemberId = 99L),
        )

        assertFailsWith<ForbiddenException> {
            service.validateFileUpload(
                fileUploadId = "file-1",
                memberId = 10L,
                apiPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `validateFileUpload는 이미 업로드 완료된 파일이면 422를 반환한다`() {
        given(fileUploadRepository.findForUpdateById("file-1")).willReturn(
            testFileUpload(isUploaded = true),
        )

        assertFailsWith<UnprocessableEntityException> {
            service.validateFileUpload(
                fileUploadId = "file-1",
                memberId = 10L,
                apiPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `validateFileUpload는 삭제 표시된 파일이면 422를 반환한다`() {
        given(fileUploadRepository.findForUpdateById("file-1")).willReturn(
            testFileUpload(deletedAt = Instant.parse("2025-01-02T00:00:00Z")),
        )

        assertFailsWith<UnprocessableEntityException> {
            service.validateFileUpload(
                fileUploadId = "file-1",
                memberId = 10L,
                apiPath = "/groups/1/activity-report/upload-link",
            )
        }
    }

    @Test
    fun `validateFileUpload는 스토리지에 파일이 없으면 404를 반환한다`() {
        given(fileUploadRepository.findForUpdateById("file-1")).willReturn(baseFileUpload)
        given(storageService.isFileExists("key-1")).willReturn(false)

        assertFailsWith<NotFoundException> {
            service.validateFileUpload(
                fileUploadId = "file-1",
                memberId = 10L,
                apiPath = "/groups/1/activity-report/upload-link",
            )
        }
    }
}
