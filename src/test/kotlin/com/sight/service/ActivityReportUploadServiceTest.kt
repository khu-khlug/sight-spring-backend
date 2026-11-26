package com.sight.service

import com.sight.core.config.ConfigKey
import com.sight.core.config.SystemConfigRegistry
import com.sight.core.exception.BadRequestException
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.testFileUpload
import com.sight.repository.FileUploadRepository
import com.sight.repository.GroupFileRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.util.Optional

class ActivityReportUploadServiceTest {
    private val fileUploadRepository = mock<FileUploadRepository>()
    private val groupFileRepository = mock<GroupFileRepository>()
    private val storageService = mock<StorageService>()
    private val fileUploadService = mock<FileUploadService>()
    private val integerIdService = mock<IntegerIdService>()
    private val systemConfigRegistry = mock<SystemConfigRegistry>()
    private val service =
        ActivityReportUploadService(
            fileUploadRepository,
            groupFileRepository,
            storageService,
            fileUploadService,
            integerIdService,
            FileUploadSizePolicyService(systemConfigRegistry),
        )
    private val fileUpload = testFileUpload()

    @BeforeEach
    fun setUp() {
        given(systemConfigRegistry.getValueAsLong(any())).willAnswer { it.getArgument<ConfigKey>(0).defaultValue.toLong() }
    }

    @Test
    fun `활동보고 설정 한도를 초과하면 공통 업로드 서비스를 호출하기 전에 거부한다`() {
        given(systemConfigRegistry.getValueAsLong(ConfigKey.GROUP_ACTIVITY_REPORT_FILE_MAX_SIZE_BYTES)).willReturn(100L)

        assertThrows<BadRequestException> {
            service.createUploadLink(1L, 10L, 101L, fileUpload.apiPath)
        }

        verifyNoInteractions(fileUploadService, groupFileRepository, storageService)
    }

    @Test
    fun `공통 한도를 초과하면 활동보고 한도 이하여도 업로드 링크를 발급하지 않는다`() {
        given(systemConfigRegistry.getValueAsLong(ConfigKey.FILE_UPLOAD_MAX_SIZE_BYTES)).willReturn(100L)

        assertThrows<BadRequestException> {
            service.createUploadLink(1L, 10L, 101L, fileUpload.apiPath)
        }

        verifyNoInteractions(fileUploadService, groupFileRepository, storageService)
    }

    @Test
    fun `활동보고 한도를 30 MiB로 높이면 같은 크기의 파일을 활동보고 용도로 발급한다`() {
        val size = 30L * 1024 * 1024
        given(systemConfigRegistry.getValueAsLong(ConfigKey.FILE_UPLOAD_MAX_SIZE_BYTES)).willReturn(size)
        given(systemConfigRegistry.getValueAsLong(ConfigKey.GROUP_ACTIVITY_REPORT_FILE_MAX_SIZE_BYTES)).willReturn(size)
        val expected = UploadLinkResult("https://upload.example.com", "key-1", fileUpload.id)
        given(fileUploadService.createUploadLink(10L, fileUpload.apiPath, size, FileReferenceType.GROUP_ACTIVITY_REPORT))
            .willReturn(expected)
        given(integerIdService.nextId()).willReturn(1L)

        val result = service.createUploadLink(1L, 10L, size, fileUpload.apiPath)

        assertEquals(expected, result)
        verify(fileUploadService).createUploadLink(10L, fileUpload.apiPath, size, FileReferenceType.GROUP_ACTIVITY_REPORT)
        verify(groupFileRepository).save(any())
    }

    @Test
    fun `업로드된 실제 크기가 활동보고 한도를 초과하면 초과 상태를 반환한다`() {
        given(systemConfigRegistry.getValueAsLong(ConfigKey.GROUP_ACTIVITY_REPORT_FILE_MAX_SIZE_BYTES)).willReturn(100L)
        given(fileUploadRepository.findById(fileUpload.id)).willReturn(Optional.of(fileUpload))
        given(storageService.getFileSize(fileUpload.fileKey)).willReturn(101L)

        val status = service.checkUploadedObject(fileUpload.id, fileUpload.uploaderMemberId, fileUpload.apiPath)

        assertEquals(ActivityReportUploadStatus.TOO_LARGE, status)
    }

    @Test
    fun `업로드된 실제 크기가 공통 한도를 초과하면 활동보고 한도 이하여도 초과 상태를 반환한다`() {
        given(systemConfigRegistry.getValueAsLong(ConfigKey.FILE_UPLOAD_MAX_SIZE_BYTES)).willReturn(100L)
        given(fileUploadRepository.findById(fileUpload.id)).willReturn(Optional.of(fileUpload))
        given(storageService.getFileSize(fileUpload.fileKey)).willReturn(101L)

        val status = service.checkUploadedObject(fileUpload.id, fileUpload.uploaderMemberId, fileUpload.apiPath)

        assertEquals(ActivityReportUploadStatus.TOO_LARGE, status)
    }

    @Test
    fun `업로드된 실제 크기가 높인 활동보고 한도와 같으면 제출 가능한 상태를 반환한다`() {
        val size = 30L * 1024 * 1024
        given(systemConfigRegistry.getValueAsLong(ConfigKey.FILE_UPLOAD_MAX_SIZE_BYTES)).willReturn(size)
        given(systemConfigRegistry.getValueAsLong(ConfigKey.GROUP_ACTIVITY_REPORT_FILE_MAX_SIZE_BYTES)).willReturn(size)
        given(fileUploadRepository.findById(fileUpload.id)).willReturn(Optional.of(fileUpload))
        given(storageService.getFileSize(fileUpload.fileKey)).willReturn(size)

        val status = service.checkUploadedObject(fileUpload.id, fileUpload.uploaderMemberId, fileUpload.apiPath)

        assertEquals(ActivityReportUploadStatus.AVAILABLE, status)
    }
}
