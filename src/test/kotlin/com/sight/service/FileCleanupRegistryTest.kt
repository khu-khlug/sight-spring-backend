package com.sight.service

import com.sight.domain.file.FileReference
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.testFileUpload
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.function.Supplier

class FileCleanupRegistryTest {
    private val attachments = mock<GroupRecordAttachmentService>()
    private val reports = mock<GroupActivityReportService>()
    private val runner =
        ApplicationContextRunner()
            .withUserConfiguration(GeneratedFileCleanupRegistry::class.java)
            .withBean(GroupRecordAttachmentService::class.java, Supplier { attachments })
            .withBean(GroupActivityReportService::class.java, Supplier { reports })

    @Test
    fun `생성한 빈은 기록과 임시저장과 활동보고의 판단 함수를 빠짐없이 연결한다`() {
        runner.run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).hasSingleBean(FileCleanupRegistry::class.java)
            val bindings = context.getBean(FileCleanupRegistry::class.java).bindings
            assertThat(bindings.map { it.referenceType }).containsExactlyInAnyOrder(
                FileReferenceType.GROUP_RECORD_ATTACHMENT,
                FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT,
                FileReferenceType.GROUP_ACTIVITY_REPORT,
            )
            bindings.forEach { binding ->
                val reference = FileReference("ref", "file-1", binding.referenceType, "3")
                val input = FileCleanupContext(testFileUpload(), reference, true)
                if (binding.referenceType == FileReferenceType.GROUP_ACTIVITY_REPORT) {
                    given(reports.isReportFileCleanupTarget(input)).willReturn(true)
                    assertThat(binding.isCleanupTarget(input)).isTrue()
                    verify(reports).isReportFileCleanupTarget(input)
                    assertThat(binding.releaseDetails).isNull()
                } else {
                    given(attachments.isAttachmentCleanupTarget(input)).willReturn(true)
                    assertThat(binding.isCleanupTarget(input)).isTrue()
                    verify(attachments).isAttachmentCleanupTarget(input)
                    assertThat(binding.releaseDetails).isNotNull()
                    binding.releaseDetails!!.invoke(reference)
                    verify(attachments).deleteAttachmentDetailsForCleanup(reference)
                }
            }
        }
    }

    @Test
    fun `서비스 빈이 누락되면 생성된 등록 빈의 시작을 실패시킨다`() {
        ApplicationContextRunner()
            .withUserConfiguration(GeneratedFileCleanupRegistry::class.java)
            .withBean(GroupRecordAttachmentService::class.java, Supplier { attachments })
            .run { context -> assertThat(context).hasFailed() }
    }
}
