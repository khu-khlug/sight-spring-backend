package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

// 그룹이 쓰는 업로드 파일. 업로드 기록은 그룹 외의 기능에서도 쓰므로 그룹 소속은 이 관계로 따로 둔다.
// 활동보고·카드 커버·기록 첨부 파일이 어느 그룹 것인지, 다른 기록에서 참조할 때 같은 그룹 파일인지 확인하는 기준이다.
@Entity
// 유니크(file_upload_id): 업로드 파일은 한 그룹에만 속한다.
// group_id 인덱스 없음: group_id로 조회하는 곳이 없다.
@Table(
    name = "khlug_group_file",
    uniqueConstraints = [UniqueConstraint(columnNames = ["file_upload_id"])],
)
class GroupFile(
    groupId: Long,
    fileUploadId: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0

    // 파일을 쓰는 그룹.
    @Column(name = "group_id", nullable = false)
    val groupId: Long = groupId

    // 업로드 기록. 업로드 파일 하나는 한 그룹에만 속한다.
    @Column(name = "file_upload_id", nullable = false, length = 100)
    val fileUploadId: String = fileUploadId

    init {
        require(groupId > 0) { "그룹 식별자가 올바르지 않습니다." }
        require(fileUploadId.isNotBlank()) { "업로드 파일 식별자가 올바르지 않습니다." }
    }
}
