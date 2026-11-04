package com.sight.domain.file

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Duration
import java.time.Instant

// 업로드 기록. R2 객체 하나와 1:1로 대응하며, 파일의 상태는 R2가 아니라 이 기록이 결정한다.
@Entity
@Table(
    name = "file_upload",
    indexes = [
        Index(columnList = "deleted_at"),
        Index(columnList = "is_uploaded, upload_check_at"),
    ],
)
class FileUpload(
    id: String,
    fileKey: String,
    uploaderMemberId: Long,
    apiPath: String,
    fileName: String?,
    size: Long?,
    contentType: String?,
    createdAt: Instant,
    uploadCheckGrace: Duration = DEFAULT_UPLOAD_CHECK_GRACE,
) {
    // 업로드 기록 식별자. 클라이언트가 제출 API에 보내는 fileUploadId다.
    @Id
    @Column(name = "id", nullable = false, length = ID_MAX_LENGTH)
    val id: String = id

    // R2 객체 key.
    @Column(name = "file_key", nullable = false, length = 255)
    val fileKey: String = fileKey

    // 업로드 링크를 발급받은 회원. 제출할 때 요청자와 같아야 한다.
    @Column(name = "member_id", nullable = false)
    val uploaderMemberId: Long = uploaderMemberId

    // 실제 아이디가 들어간 발급 경로. 제출할 때 용도와 대상 리소스가 맞는지 확인하는 기준이다.
    @Column(name = "api_path", nullable = false, length = 255)
    val apiPath: String = apiPath

    // 원본 파일명. 발급 요청에 파일명이 없는 용도면 null이다.
    @Column(name = "file_name", length = 255)
    val fileName: String? = fileName

    // 바이트 크기. 발급 요청에 크기가 없는 용도면 null이다.
    @Column(name = "size")
    val size: Long? = size

    // MIME 타입. 발급 요청에 MIME 타입이 없는 용도면 null이다.
    @Column(name = "content_type", length = 255)
    val contentType: String? = contentType

    // 업로드 완료 표시. R2에 객체가 있음을 확인해 업로드 링크가 사용된 상태다.
    // 활동보고·카드 커버는 제출할 때, 기록 첨부는 업로드 완료 알림이나 업로드 확인 예약에서 남긴다.
    @Column(name = "is_uploaded", nullable = false)
    var isUploaded: Boolean = false
        private set

    // 업로드 확인 예약 시각. 링크 만료 시각에 확인 여유 시간을 더한 값이며, 이때까지 R2에 객체가 없으면 정리한다.
    @Column(name = "upload_check_at", nullable = false)
    val uploadCheckAt: Instant = createdAt.plus(UPLOAD_LINK_VALIDITY).plus(uploadCheckGrace)

    // 삭제 표시 시각. null이 아니면 유효한 파일이 아니며 R2 삭제를 기다리는 상태다.
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
        private set

    // 업로드 링크 발급 일시.
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = createdAt

    init {
        require(id.isNotBlank() && id.length <= ID_MAX_LENGTH) { "업로드 기록 식별자가 올바르지 않습니다." }
        require(fileKey.isNotBlank()) { "파일 key가 필요합니다." }
        require(uploaderMemberId > 0) { "회원 식별자가 올바르지 않습니다." }
        require(apiPath.isNotBlank()) { "발급 경로가 필요합니다." }
        require(fileName == null || fileName.isNotBlank()) { "파일명이 올바르지 않습니다." }
        require(size == null || size in 0..MAX_FILE_SIZE) { "파일 크기는 ${MAX_FILE_SIZE}바이트 이하여야 합니다." }
        require(contentType == null || contentType.isNotBlank()) { "MIME 타입이 올바르지 않습니다." }
        require(!uploadCheckGrace.isNegative) { "업로드 확인 여유 시간이 올바르지 않습니다." }
    }

    val isDeleted: Boolean
        get() = deletedAt != null

    // 업로드 링크가 만료되는 시각.
    val uploadLinkExpiresAt: Instant
        get() = createdAt.plus(UPLOAD_LINK_VALIDITY)

    fun isIssuedBy(memberId: Long): Boolean = uploaderMemberId == memberId

    // 업로드 완료 표시를 남긴다. 이미 표시돼 있으면 false를 반환한다.
    fun markUploaded(): Boolean {
        require(!isDeleted) { "삭제 처리된 파일입니다." }
        if (isUploaded) return false
        isUploaded = true
        return true
    }

    // 삭제 표시를 남긴다. 커밋 후 R2 객체를 지우고 성공하면 업로드 기록을 지운다.
    fun markDeleted(at: Instant) {
        require(!isDeleted) { "이미 삭제 처리된 파일입니다." }
        deletedAt = at
    }

    companion object {
        const val ID_MAX_LENGTH: Int = 100

        // 파일 하나의 최대 크기. 정확히 같은 크기는 허용한다.
        const val MAX_FILE_SIZE: Long = 20L * 1024 * 1024

        val UPLOAD_LINK_VALIDITY: Duration = Duration.ofMinutes(10)
        val DEFAULT_UPLOAD_CHECK_GRACE: Duration = Duration.ofMinutes(1)

        // 읽기 링크 유효 시간. R2 Presigned GET URL의 최대치다.
        val READ_LINK_VALIDITY: Duration = Duration.ofDays(7)
    }
}
