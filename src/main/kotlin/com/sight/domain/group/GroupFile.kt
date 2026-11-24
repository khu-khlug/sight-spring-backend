package com.sight.domain.group

import com.sight.domain.file.FileUpload
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

// 업로드 파일의 그룹 소속 정보. 열람 권한의 기준이며 사용 여부는 공통 파일 참조로 판단한다.
// 활동보고·카드 커버·기록 첨부 파일이 어느 그룹 것인지, 다른 기록에서 참조할 때 같은 그룹 파일인지 확인하는 기준이다.
@Entity
// 유니크(file_upload_id): 업로드 파일은 한 그룹에만 속한다.
// group_id 인덱스 없음: group_id로 조회하는 곳이 없다.
@Table(
    name = "khlug_group_file",
    uniqueConstraints = [UniqueConstraint(columnNames = ["file_upload_id"])],
)
class GroupFile(
    id: Long,
    groupId: Long,
    fileUploadId: String,
) {
    @Id
    @Column(name = "id", nullable = false)
    val id: Long = id

    // 파일을 쓰는 그룹.
    @Column(name = "group_id", nullable = false)
    val groupId: Long = groupId

    // 업로드 기록. 업로드 파일 하나는 한 그룹에만 속한다.
    @Column(name = "file_upload_id", nullable = false, length = 100)
    val fileUploadId: String = fileUploadId

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "file_upload_id",
        insertable = false,
        updatable = false,
        nullable = false,
        foreignKey = ForeignKey(name = "fk_group_file_upload"),
    )
    private var fileUpload: FileUpload? = null

    init {
        require(id > 0) { "식별자가 올바르지 않습니다." }
        require(groupId > 0) { "그룹 식별자가 올바르지 않습니다." }
        require(fileUploadId.isNotBlank()) { "업로드 파일 식별자가 올바르지 않습니다." }
    }
}
