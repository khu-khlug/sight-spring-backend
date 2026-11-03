package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant

@Entity
@Table(
    name = "khlug_group_card",
    indexes = [
        Index(columnList = "list_id, position, id"),
        Index(columnList = "group_id, deleted_at, id"),
    ],
)
class GroupCard(
    id: String,
    groupId: Long,
    listId: String,
    authorMemberId: Long,
    title: String,
    position: Int,
    createdAt: Instant,
) {
    // 카드 식별자. 그룹 API 아이디 기준에 따라 문자열(ULID)이다.
    @Id
    @Column(name = "id", nullable = false, length = ID_MAX_LENGTH)
    val id: String = id

    // 소속 그룹. 카드를 다른 그룹으로 옮길 수 없으므로 바뀌지 않는다.
    @Column(name = "group_id", nullable = false)
    val groupId: Long = groupId

    // 소속 리스트. 삭제된 카드는 리스트에서 빠지므로 null이고, 복구하면 지정한 리스트로 들어간다.
    @Column(name = "list_id", length = GroupList.ID_MAX_LENGTH)
    var listId: String? = listId
        private set

    // 리스트 안의 표시 순서. 오름차순으로 보여 주며 같으면 카드 아이디 오름차순이다. 삭제된 카드는 null이다.
    @Column(name = "position")
    var position: Int? = position
        private set

    // 카드를 만든 회원. 카드 삭제 시 경험치를 차감하는 대상이다.
    @Column(name = "author_member_id", nullable = false)
    val authorMemberId: Long = authorMemberId

    // 카드 이름. 앞뒤 공백을 제거해 저장한다.
    @Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
    var title: String = title
        private set

    // 카드 설명 Markdown 원문. 서버는 파싱·정제하지 않으며 없으면 빈 문자열이다.
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    var description: String = ""
        private set

    // 담당 회원. 미지정이면 null이다.
    @Column(name = "assignee_member_id")
    var assigneeMemberId: Long? = null
        private set

    // 외부 이미지 커버 주소. 업로드 파일 커버와 동시에 가질 수 없다.
    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    var coverImageUrl: String? = null
        private set

    // 업로드한 커버 파일의 업로드 기록. 외부 이미지 커버와 동시에 가질 수 없다.
    @Column(name = "cover_file_upload_id", length = 100)
    var coverFileUploadId: String? = null
        private set

    // 레이블 비트마스크. API에는 레이블 목록으로만 노출한다.
    @Column(name = "label_bits", nullable = false)
    var labelBits: Int = 0
        private set

    // 카드의 포트폴리오 포함 여부. 그룹 포트폴리오 발행 여부와는 별개다.
    @Column(name = "is_in_portfolio", nullable = false)
    var isInPortfolio: Boolean = false
        private set

    // 비활성 여부. 삭제와 달리 목록에 남으며 내용 변경과 기록 변경을 막는다.
    @Column(name = "is_disabled", nullable = false)
    var isDisabled: Boolean = false
        private set

    // 카드별 기록 정렬 설정. 회원 개인 설정이 아니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "record_sort", nullable = false, length = 20)
    var recordSort: GroupRecordSort = GroupRecordSort.NEWEST
        private set

    // 이 카드가 직접 소유한 삭제되지 않은 기록 수.
    @Column(name = "record_count", nullable = false)
    var recordCount: Long = 0
        private set

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = createdAt

    // 카드 행이 마지막으로 바뀐 일시. 기록 추가·삭제로 기록 수가 바뀔 때도 갱신하며, 검색의 최근 변경 정렬 기준이다.
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = createdAt
        private set

    // 소프트 삭제 일시. null이 아니면 삭제된 카드다.
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
        private set

    // JPA 낙관적 잠금용 수정 버전. 위치·기록 수·상태의 동시 변경 충돌을 저장 시 감지한다.
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        private set

    init {
        require(id.isNotBlank() && id.length <= ID_MAX_LENGTH) { "카드 식별자가 올바르지 않습니다." }
        require(groupId > 0) { "그룹 식별자가 올바르지 않습니다." }
        require(listId.isNotBlank()) { "리스트 식별자가 올바르지 않습니다." }
        require(authorMemberId > 0) { "회원 식별자가 올바르지 않습니다." }
        this.title = normalizeTitle(title)
    }

    val isDeleted: Boolean
        get() = deletedAt != null

    val labels: Set<GroupCardLabel>
        get() = GroupCardLabel.fromBits(labelBits)

    val hasDescription: Boolean
        get() = description.isNotEmpty()

    // 비활성·삭제되지 않아 내용과 기록을 바꿀 수 있는 카드인지 확인한다.
    fun requireEditable() {
        require(!isDeleted) { "삭제된 카드입니다." }
        require(!isDisabled) { "비활성 카드입니다." }
    }

    // 보낸 항목만 바꾼다. 활동 로그는 이름·설명이 바뀐 경우에만 남기므로 둘을 구분해 반환한다.
    fun update(
        title: String?,
        description: String?,
        recordSort: GroupRecordSort?,
        at: Instant,
    ): UpdateResult {
        require(title != null || description != null || recordSort != null) { "수정할 항목이 필요합니다." }
        requireEditable()
        val newTitle = title?.let { normalizeTitle(it) } ?: this.title
        val newDescription = description ?: this.description
        require(newDescription.toByteArray(Charsets.UTF_8).size <= DESCRIPTION_MAX_BYTES) {
            "카드 설명은 ${DESCRIPTION_MAX_BYTES}바이트 이하여야 합니다."
        }
        val newRecordSort = recordSort ?: this.recordSort

        val result = UpdateResult(
            isContentChanged = newTitle != this.title || newDescription != this.description,
            isRecordSortChanged = newRecordSort != this.recordSort,
        )
        this.title = newTitle
        this.description = newDescription
        this.recordSort = newRecordSort
        if (result.isChanged) updatedAt = at
        return result
    }

    // 담당자를 지정하거나 해제한다. 같은 담당자면 false를 반환한다.
    fun changeAssignee(memberId: Long?, at: Instant): Boolean {
        require(memberId == null || memberId > 0) { "회원 식별자가 올바르지 않습니다." }
        requireEditable()
        if (assigneeMemberId == memberId) return false
        assigneeMemberId = memberId
        updatedAt = at
        return true
    }

    // 외부 이미지 주소를 커버로 지정한다. 같은 주소면 false를 반환한다.
    // 기존 커버가 업로드 파일이었으면 그 파일의 삭제는 호출한 쪽이 같은 트랜잭션에서 처리한다.
    fun changeCoverToImageUrl(url: String, at: Instant): Boolean {
        require(HTTP_URL_PATTERN.matches(url)) { "커버 이미지 주소는 http 또는 https 주소여야 합니다." }
        requireEditable()
        if (coverImageUrl == url) return false
        coverImageUrl = url
        coverFileUploadId = null
        updatedAt = at
        return true
    }

    // 업로드 파일을 커버로 지정한다. 업로드 파일은 매번 새로 발급받으므로 항상 바뀐다.
    fun changeCoverToFile(fileUploadId: String, at: Instant) {
        require(fileUploadId.isNotBlank()) { "업로드 파일 식별자가 올바르지 않습니다." }
        requireEditable()
        require(coverFileUploadId != fileUploadId) { "이미 커버로 사용 중인 파일입니다." }
        coverFileUploadId = fileUploadId
        coverImageUrl = null
        updatedAt = at
    }

    // 커버를 제거한다. 커버가 없으면 false를 반환한다.
    fun removeCover(at: Instant): Boolean {
        requireEditable()
        if (coverImageUrl == null && coverFileUploadId == null) return false
        coverImageUrl = null
        coverFileUploadId = null
        updatedAt = at
        return true
    }

    // 레이블 전체를 교체한다. 같은 레이블이면 false를 반환한다.
    fun changeLabels(labels: Set<GroupCardLabel>, at: Instant): Boolean {
        requireEditable()
        val bits = GroupCardLabel.toBits(labels)
        if (labelBits == bits) return false
        labelBits = bits
        updatedAt = at
        return true
    }

    // 비활성화하거나 다시 활성화한다. 같은 상태면 false를 반환한다.
    fun changeDisabled(isDisabled: Boolean, at: Instant): Boolean {
        require(!isDeleted) { "삭제된 카드입니다." }
        if (this.isDisabled == isDisabled) return false
        this.isDisabled = isDisabled
        updatedAt = at
        return true
    }

    // 포트폴리오 포함 여부를 바꾼다. 같은 값이면 false를 반환한다.
    fun changePortfolio(isInPortfolio: Boolean, at: Instant): Boolean {
        requireEditable()
        if (this.isInPortfolio == isInPortfolio) return false
        this.isInPortfolio = isInPortfolio
        updatedAt = at
        return true
    }

    // 같은 그룹의 리스트 안 위치로 옮긴다. 비활성 카드도 옮길 수 있다. 위치 계산은 호출한 쪽이 한다.
    fun moveTo(listId: String, position: Int, at: Instant): Boolean {
        require(listId.isNotBlank()) { "리스트 식별자가 올바르지 않습니다." }
        require(!isDeleted) { "삭제된 카드입니다." }
        if (this.listId == listId && this.position == position) return false
        this.listId = listId
        this.position = position
        updatedAt = at
        return true
    }

    // 소프트 삭제한다. 기록과 첨부 파일은 복구를 위해 보존하며 리스트에서는 빠진다.
    fun delete(at: Instant) {
        require(!isDeleted) { "이미 삭제된 카드입니다." }
        listId = null
        position = null
        deletedAt = at
        updatedAt = at
    }

    // 삭제된 카드를 지정한 리스트의 위치로 복구한다.
    fun restore(listId: String, position: Int, at: Instant) {
        require(listId.isNotBlank()) { "리스트 식별자가 올바르지 않습니다." }
        require(isDeleted) { "삭제된 카드가 아닙니다." }
        this.listId = listId
        this.position = position
        deletedAt = null
        updatedAt = at
    }

    fun increaseRecordCount(at: Instant) {
        recordCount += 1
        updatedAt = at
    }

    fun decreaseRecordCount(at: Instant) {
        require(recordCount > 0) { "기록 수는 음수가 될 수 없습니다." }
        recordCount -= 1
        updatedAt = at
    }

    data class UpdateResult(
        // 이름 또는 설명이 바뀌었는지 여부. 카드 수정 로그와 최근 활동 일시 갱신 기준이다.
        val isContentChanged: Boolean,
        // 기록 정렬 설정이 바뀌었는지 여부. 별도 로그를 남기지 않는다.
        val isRecordSortChanged: Boolean,
    ) {
        val isChanged: Boolean
            get() = isContentChanged || isRecordSortChanged
    }

    companion object {
        const val ID_MAX_LENGTH: Int = 100
        const val TITLE_MAX_LENGTH: Int = 255
        const val DESCRIPTION_MAX_BYTES: Int = 4096

        private val HTTP_URL_PATTERN = Regex("^https?://\\S+$", RegexOption.IGNORE_CASE)

        private fun normalizeTitle(title: String): String {
            val trimmed = title.trim()
            require(trimmed.isNotEmpty()) { "카드 이름이 필요합니다." }
            require(trimmed.codePointCount(0, trimmed.length) <= TITLE_MAX_LENGTH) {
                "카드 이름은 ${TITLE_MAX_LENGTH}자 이하여야 합니다."
            }
            return trimmed
        }
    }
}
