package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

// 회원의 기록 임시저장. 회원당 하나뿐이며 저장하면 기존 임시저장을 대체한다.
@Entity
// 유니크(member_id): 임시저장은 회원당 하나.
@Table(
    name = "khlug_group_record_draft",
    uniqueConstraints = [UniqueConstraint(columnNames = ["member_id"])],
)
class GroupRecordDraft(
    memberId: Long,
    groupId: Long,
    cardId: Long,
    recordId: Long?,
    content: String,
    updatedAt: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0

    // 임시저장한 회원.
    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    // 임시저장이 속한 그룹. 경로의 그룹과 같을 때만 조회·삭제한다.
    @Column(name = "group_id", nullable = false)
    var groupId: Long = groupId
        private set

    // 작성 중인 기록이 속한 카드.
    @Column(name = "card_id", nullable = false)
    var cardId: Long = cardId
        private set

    // 수정 중인 기록. 새 기록 작성 중이면 null이다.
    @Column(name = "record_id")
    var recordId: Long? = recordId
        private set

    // 임시저장 본문. TIPTAP 문서 JSON 직렬화 문자열이며 서버는 파싱하지 않는다.
    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    var content: String = content
        private set

    // 클라이언트에서 임시저장 내용을 마지막으로 수정한 일시. 이보다 이른 저장 요청은 더 오래된 내용이므로 거부한다.
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = updatedAt
        private set

    init {
        require(memberId > 0) { "회원 식별자가 올바르지 않습니다." }
        validateTarget(groupId, cardId, recordId)
        validateContent(content)
    }

    val isNewRecord: Boolean
        get() = recordId == null

    fun belongsTo(groupId: Long): Boolean = this.groupId == groupId

    // 같은 그룹·카드·기록을 대상으로 하는지 여부. 다르면 기존 첨부 연결을 모두 해제해야 한다.
    fun isSameTarget(
        groupId: Long,
        cardId: Long,
        recordId: Long?,
    ): Boolean = this.groupId == groupId && this.cardId == cardId && this.recordId == recordId

    // 서버에 저장된 것보다 이른 시각의 내용인지 여부. 이르면 덮어쓰지 않는다.
    fun isNewerThan(updatedAt: Instant): Boolean = this.updatedAt.isAfter(updatedAt)

    // 새 내용으로 덮어쓴다. 서버에 더 최신 내용이 있으면 거부한다.
    fun overwrite(
        groupId: Long,
        cardId: Long,
        recordId: Long?,
        content: String,
        updatedAt: Instant,
    ) {
        require(!isNewerThan(updatedAt)) { "서버에 더 최신 임시저장이 있습니다." }
        validateTarget(groupId, cardId, recordId)
        validateContent(content)
        this.groupId = groupId
        this.cardId = cardId
        this.recordId = recordId
        this.content = content
        this.updatedAt = updatedAt
    }

    companion object {
        private fun validateTarget(
            groupId: Long,
            cardId: Long,
            recordId: Long?,
        ) {
            require(groupId > 0) { "그룹 식별자가 올바르지 않습니다." }
            require(cardId > 0) { "카드 식별자가 올바르지 않습니다." }
            require(recordId == null || recordId > 0) { "기록 식별자가 올바르지 않습니다." }
        }

        private fun validateContent(content: String) {
            require(content.toByteArray(Charsets.UTF_8).size <= GroupRecord.CONTENT_MAX_BYTES) {
                "임시저장 본문은 ${GroupRecord.CONTENT_MAX_BYTES}바이트 이하여야 합니다."
            }
        }
    }
}
