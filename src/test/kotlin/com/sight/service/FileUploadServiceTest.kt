package com.sight.service

import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.file.FileUpload
import com.sight.repository.FileUploadRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.Optional
import kotlin.test.assertFailsWith

class FileUploadServiceTest {
    private val fileUploadRepository = mock<FileUploadRepository>()
    private val storageService = mock<StorageService>()
    private lateinit var service: FileUploadService

    private val baseFileUpload =
        FileUpload(
            id = "file-1",
            fileKey = "key-1",
            memberId = 10L,
            apiPath = "/groups/1/activity-report/upload-link",
        )

    @BeforeEach
    fun setUp() {
        service = FileUploadService(fileUploadRepository, storageService)
        given(storageService.generateUploadUrl(any())).willReturn("https://upload.example.com")
        given(storageService.isFileExists(any())).willReturn(true)
        given(fileUploadRepository.save(any<FileUpload>())).willReturn(baseFileUpload)
    }

    // ── createUploadLink ──────────────────────────────────────────────────────

    @Test
    fun `createUploadLink는 FileUpload를 저장하고 url, fileKey, fileUploadId를 반환한다`() {
        val result = service.createUploadLink(memberId = 10L, apiPath = "/groups/1/activity-report/upload-link")

        verify(fileUploadRepository).save(argThat { memberId == 10L && apiPath == "/groups/1/activity-report/upload-link" })
        assert(result.url == "https://upload.example.com")
        assert(result.fileUploadId == "file-1")
    }

    // ── validateFileUpload ────────────────────────────────────────────────────

    @Test
    fun `validateFileUpload는 유효한 파일이면 FileUpload를 반환한다`() {
        given(fileUploadRepository.findById("file-1")).willReturn(Optional.of(baseFileUpload))

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
        given(fileUploadRepository.findById("file-1")).willReturn(Optional.empty())

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
        given(fileUploadRepository.findById("file-1")).willReturn(
            Optional.of(baseFileUpload.copy(apiPath = "/other/path")),
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
    fun `validateFileUpload는 memberId가 불일치하면 403을 반환한다`() {
        given(fileUploadRepository.findById("file-1")).willReturn(
            Optional.of(baseFileUpload.copy(memberId = 99L)),
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
    fun `validateFileUpload는 isUsed가 true이면 422를 반환한다`() {
        given(fileUploadRepository.findById("file-1")).willReturn(
            Optional.of(baseFileUpload.copy(isUsed = true)),
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
        given(fileUploadRepository.findById("file-1")).willReturn(Optional.of(baseFileUpload))
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
