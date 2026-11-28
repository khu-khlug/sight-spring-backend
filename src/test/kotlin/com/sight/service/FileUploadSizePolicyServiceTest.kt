package com.sight.service

import com.sight.core.config.ConfigKey
import com.sight.core.config.SystemConfigRegistry
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.InvalidConfigValueException
import com.sight.domain.file.FileReferenceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock

class FileUploadSizePolicyServiceTest {
    private val systemConfigRegistry = mock<SystemConfigRegistry>()
    private val service = FileUploadSizePolicyService(systemConfigRegistry)

    @BeforeEach
    fun setUp() {
        given(systemConfigRegistry.getValueAsLong(any())).willAnswer { it.getArgument<ConfigKey>(0).defaultValue.toLong() }
    }

    @Test
    fun `용도별 최대 크기는 해당 설정과 공통 설정 중 작은 값을 사용한다`() {
        given(systemConfigRegistry.getValueAsLong(ConfigKey.FILE_UPLOAD_MAX_SIZE_BYTES)).willReturn(100L)
        given(systemConfigRegistry.getValueAsLong(ConfigKey.GROUP_RECORD_FILE_MAX_SIZE_BYTES)).willReturn(80L)
        given(systemConfigRegistry.getValueAsLong(ConfigKey.GROUP_ACTIVITY_REPORT_FILE_MAX_SIZE_BYTES)).willReturn(120L)
        given(systemConfigRegistry.getValueAsLong(ConfigKey.GROUP_CARD_COVER_IMAGE_MAX_SIZE_BYTES)).willReturn(40L)

        assertEquals(80L, service.getMaxSizeBytes(FileReferenceType.GROUP_RECORD_ATTACHMENT))
        assertEquals(80L, service.getMaxSizeBytes(FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT))
        assertEquals(100L, service.getMaxSizeBytes(FileReferenceType.GROUP_ACTIVITY_REPORT))
        assertEquals(40L, service.getMaxSizeBytes(FileReferenceType.GROUP_CARD_COVER))
    }

    @Test
    fun `설정한 최대 크기와 같은 파일은 허용하고 1바이트 초과한 파일은 거부한다`() {
        given(systemConfigRegistry.getValueAsLong(ConfigKey.FILE_UPLOAD_MAX_SIZE_BYTES)).willReturn(10L)

        service.validateSize(10L)
        assertThrows<BadRequestException> { service.validateSize(11L) }
    }

    @Test
    fun `파일 크기가 음수이면 거부한다`() {
        assertThrows<BadRequestException> { service.validateSize(-1L) }
    }

    @Test
    fun `공통 최대 크기 설정이 음수이면 설정 오류로 처리한다`() {
        given(systemConfigRegistry.getValueAsLong(ConfigKey.FILE_UPLOAD_MAX_SIZE_BYTES)).willReturn(-1L)

        assertThrows<InvalidConfigValueException> { service.getMaxSizeBytes() }
    }

    @Test
    fun `용도별 최대 크기 설정이 음수이면 설정 오류로 처리한다`() {
        given(systemConfigRegistry.getValueAsLong(ConfigKey.GROUP_ACTIVITY_REPORT_FILE_MAX_SIZE_BYTES)).willReturn(-1L)

        assertThrows<InvalidConfigValueException> { service.getMaxSizeBytes(FileReferenceType.GROUP_ACTIVITY_REPORT) }
    }
}
