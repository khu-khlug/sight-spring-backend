package com.sight.domain.file

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    name = "file_reference",
    indexes = [
        Index(name = "idx_file_reference_upload", columnList = "file_upload_id"),
        Index(name = "idx_file_reference_owner", columnList = "reference_type, owner_id"),
    ],
)
class FileReference(
    id: String,
    fileUploadId: String,
    referenceType: FileReferenceType,
    ownerId: String,
) {
    @Id
    @Column(name = "id", nullable = false, length = ID_MAX_LENGTH)
    val id: String = id

    @Column(name = "file_upload_id", nullable = false, length = FileUpload.ID_MAX_LENGTH)
    val fileUploadId: String = fileUploadId

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "file_upload_id",
        insertable = false,
        updatable = false,
        nullable = false,
        foreignKey = ForeignKey(name = "fk_file_reference_upload"),
    )
    private var fileUpload: FileUpload? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    val referenceType: FileReferenceType = referenceType

    // 소유자 식별자는 참조 종류가 정한 리소스의 식별자 원문이다.
    @Column(
        name = "owner_id",
        nullable = false,
        length = OWNER_ID_MAX_LENGTH,
        columnDefinition = "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin",
    )
    val ownerId: String = ownerId

    init {
        require(id.isNotBlank() && id.length <= ID_MAX_LENGTH) { "파일 참조 식별자가 올바르지 않습니다." }
        require(fileUploadId.isNotBlank() && fileUploadId.length <= FileUpload.ID_MAX_LENGTH) {
            "업로드 파일 식별자가 올바르지 않습니다."
        }
        require(ownerId.isNotBlank() && ownerId.length <= OWNER_ID_MAX_LENGTH) { "소유자 식별자가 올바르지 않습니다." }
    }

    companion object {
        const val ID_MAX_LENGTH: Int = 100
        const val OWNER_ID_MAX_LENGTH: Int = 100
    }
}
