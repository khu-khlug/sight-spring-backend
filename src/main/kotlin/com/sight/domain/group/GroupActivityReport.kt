package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "khlug_group_activity_report",
    uniqueConstraints = [UniqueConstraint(columnNames = ["group_id", "seminar_id"])],
)
class GroupActivityReport(
    id: String,
    groupId: Long,
    seminarId: String,
    isPresentation: Boolean,
    reportFileUploadId: String,
    createdAt: Instant,
) {
    // 활동보고 식별자.
    @Id
    @Column(name = "id", nullable = false, length = ID_MAX_LENGTH)
    val id: String = id

    // 제출한 그룹. 세미나와 함께 유일해 같은 세미나에 한 번만 제출한다.
    @Column(name = "group_id", nullable = false)
    val groupId: Long = groupId

    // 제출 대상 세미나.
    @Column(name = "seminar_id", nullable = false, length = 100)
    val seminarId: String = seminarId

    // true면 세미나 발표, false면 보고서.
    @Column(name = "is_presentation", nullable = false)
    var isPresentation: Boolean = isPresentation
        private set

    // 보고 파일의 업로드 기록. 파일 key와 읽기 링크는 이 업로드 기록에서 얻는다.
    @Column(name = "report_file_upload_id", nullable = false, length = 100)
    var reportFileUploadId: String = reportFileUploadId
        private set

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = createdAt

    // 유형이나 파일이 마지막으로 바뀐 일시.
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = createdAt
        private set

    init {
        require(id.isNotBlank() && id.length <= ID_MAX_LENGTH) { "활동보고 식별자가 올바르지 않습니다." }
        require(groupId > 0) { "그룹 식별자가 올바르지 않습니다." }
        require(seminarId.isNotBlank()) { "세미나 식별자가 올바르지 않습니다." }
        require(reportFileUploadId.isNotBlank()) { "업로드 파일 식별자가 올바르지 않습니다." }
    }

    // 보낸 항목만 바꾼다. 알림 종류를 정하도록 실제로 바뀐 항목을 구분해 반환한다.
    fun change(
        isPresentation: Boolean?,
        reportFileUploadId: String?,
        at: Instant,
    ): ChangeResult {
        require(isPresentation != null || reportFileUploadId != null) { "수정할 항목이 필요합니다." }
        require(reportFileUploadId == null || reportFileUploadId.isNotBlank()) { "업로드 파일 식별자가 올바르지 않습니다." }
        val result =
            ChangeResult(
                isPresentationChanged = isPresentation != null && isPresentation != this.isPresentation,
                isFileChanged = reportFileUploadId != null && reportFileUploadId != this.reportFileUploadId,
            )
        if (isPresentation != null) this.isPresentation = isPresentation
        if (reportFileUploadId != null) this.reportFileUploadId = reportFileUploadId
        if (result.isChanged) updatedAt = at
        return result
    }

    data class ChangeResult(
        val isPresentationChanged: Boolean,
        val isFileChanged: Boolean,
    ) {
        val isChanged: Boolean
            get() = isPresentationChanged || isFileChanged
    }

    companion object {
        const val ID_MAX_LENGTH: Int = 100
    }
}
