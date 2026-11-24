package com.sight.domain.group

import com.sight.domain.file.FileReference
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

// FileReference의 부가 정보인 파일 블록 아이디를 저장하는 테이블이다.
// 파일 블록 아이디는 본문의 각 파일 블록과 업로드 파일을 대응시키는 데 필요하다.
// 같은 업로드 파일을 여러 블록에서 사용해도 블록별로 구분할 수 있다.
@Entity
@Table(
    name = "khlug_group_record_file",
    uniqueConstraints = [UniqueConstraint(name = "ux_group_record_file_reference", columnNames = ["file_reference_id"])],
)
class GroupRecordFile(
    id: Long,
    fileReferenceId: String,
    fileBlockId: String,
) {
    @Id
    @Column(name = "id", nullable = false)
    val id: Long = id

    @Column(name = "file_reference_id", nullable = false, length = FileReference.ID_MAX_LENGTH)
    val fileReferenceId: String = fileReferenceId

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "file_reference_id",
        insertable = false,
        updatable = false,
        nullable = false,
        foreignKey = ForeignKey(name = "fk_group_record_file_reference"),
    )
    private var fileReference: FileReference? = null

    // 같은 파일을 여러 블록에서 사용해도 각 블록은 별도 참조를 가진다.
    @Column(name = "file_block_id", nullable = false, length = FILE_BLOCK_ID_MAX_LENGTH)
    val fileBlockId: String = fileBlockId

    init {
        require(id > 0) { "식별자가 올바르지 않습니다." }
        require(fileReferenceId.isNotBlank() && fileReferenceId.length <= FileReference.ID_MAX_LENGTH) {
            "파일 참조 식별자가 올바르지 않습니다."
        }
        require(fileBlockId.isNotBlank() && fileBlockId.length <= FILE_BLOCK_ID_MAX_LENGTH) {
            "파일 블록 아이디가 올바르지 않습니다."
        }
    }

    companion object {
        const val FILE_BLOCK_ID_MAX_LENGTH: Int = 100
    }
}
