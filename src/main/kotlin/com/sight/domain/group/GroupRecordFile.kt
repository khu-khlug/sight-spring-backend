package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

// 기록 첨부 파일의 연결 행. "소유자 + 기록 파일 식별자 → 업로드 기록"을 저장한다.
// 소유자는 기록 또는 회원의 임시저장 중 정확히 하나다. 업로드 기록을 가리키는 연결 행이 하나 이상이면 그 파일은 사용 중이다.
@Entity
@Table(
    name = "khlug_group_record_file",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["record_id", "file_identifier"]),
        UniqueConstraint(columnNames = ["draft_id", "file_identifier"]),
    ],
    indexes = [Index(columnList = "file_upload_id")],
)
class GroupRecordFile private constructor(
    recordId: Long?,
    draftId: Long?,
    fileIdentifier: String,
    fileUploadId: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0

    // 소유 기록. 임시저장 소유면 null이다.
    @Column(name = "record_id")
    val recordId: Long? = recordId

    // 소유 임시저장. 기록 소유면 null이다.
    @Column(name = "draft_id")
    val draftId: Long? = draftId

    // 본문의 파일 블록이 가진 기록 파일 식별자. 프런트가 정하며 한 소유자 안에서만 유일하다.
    @Column(name = "file_identifier", nullable = false, length = FILE_IDENTIFIER_MAX_LENGTH)
    val fileIdentifier: String = fileIdentifier

    // 연결된 업로드 기록. 같은 그룹의 여러 기록·임시저장이 한 업로드 기록을 함께 가리킬 수 있다.
    @Column(name = "file_upload_id", nullable = false, length = 100)
    val fileUploadId: String = fileUploadId

    init {
        require((recordId == null) != (draftId == null)) { "연결 소유자는 기록 또는 임시저장 중 하나여야 합니다." }
        require(recordId == null || recordId > 0) { "기록 식별자가 올바르지 않습니다." }
        require(draftId == null || draftId > 0) { "임시저장 식별자가 올바르지 않습니다." }
        require(fileIdentifier.isNotBlank() && fileIdentifier.length <= FILE_IDENTIFIER_MAX_LENGTH) {
            "기록 파일 식별자가 올바르지 않습니다."
        }
        require(fileUploadId.isNotBlank()) { "업로드 파일 식별자가 올바르지 않습니다." }
    }

    companion object {
        const val FILE_IDENTIFIER_MAX_LENGTH: Int = 100

        fun ofRecord(recordId: Long, fileIdentifier: String, fileUploadId: String): GroupRecordFile =
            GroupRecordFile(recordId = recordId, draftId = null, fileIdentifier = fileIdentifier, fileUploadId = fileUploadId)

        fun ofDraft(draftId: Long, fileIdentifier: String, fileUploadId: String): GroupRecordFile =
            GroupRecordFile(recordId = null, draftId = draftId, fileIdentifier = fileIdentifier, fileUploadId = fileUploadId)
    }
}
