package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant

@Entity
@Table(
    name = "khlug_group_record",
    indexes = [Index(columnList = "card_id, deleted_at")],
)
class GroupRecord(
    cardId: String,
    authorMemberId: Long,
    content: String,
    createdAt: Instant,
) {
    // 기록 식별자. 그룹 API 아이디 기준에 따라 숫자(Long)다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0

    // 현재 소속 카드. 같은 그룹의 다른 카드로 옮길 수 있으며, 기록의 그룹은 이 카드의 그룹으로 판단한다.
    @Column(name = "card_id", nullable = false, length = GroupCard.ID_MAX_LENGTH)
    var cardId: String = cardId
        private set

    // 작성자. 수정 권한과 기록 삭제 시 경험치 차감 대상의 기준이다.
    @Column(name = "author_member_id", nullable = false)
    val authorMemberId: Long = authorMemberId

    // 본문 형식. 새 기록은 TIPTAP이며 LEGACY는 수정할 수 없다.
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    val type: GroupRecordType = GroupRecordType.TIPTAP

    // 본문 원문. TIPTAP은 문서 JSON 직렬화 문자열, LEGACY는 HTML이며 서버는 파싱·정제하지 않는다.
    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    var content: String = content
        private set

    // 기록의 포트폴리오 포함 여부. 그룹 발행 여부·카드 포함 여부와는 별개다.
    @Column(name = "is_in_portfolio", nullable = false)
    var isInPortfolio: Boolean = true
        private set

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = createdAt

    // 본문 또는 첨부 연결 목록이 마지막으로 바뀐 일시. 기록 목록의 최근 수정순 정렬 기준이다.
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = createdAt
        private set

    // 소프트 삭제 일시. null이 아니면 삭제된 기록이다.
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
        private set

    // JPA 낙관적 잠금용 수정 버전. 본문·소속·삭제 상태의 동시 변경 충돌을 저장 시 감지한다.
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        private set

    init {
        require(cardId.isNotBlank()) { "카드 식별자가 올바르지 않습니다." }
        require(authorMemberId > 0) { "회원 식별자가 올바르지 않습니다." }
        validateContent(content)
    }

    val isDeleted: Boolean
        get() = deletedAt != null

    fun isAuthor(memberId: Long): Boolean = authorMemberId == memberId

    // 본문을 바꾼다. 첨부 연결 목록이 바뀌었는지는 호출한 쪽이 알려 주며, 둘 중 하나라도 바뀌면 수정 일시를 갱신한다.
    fun edit(
        content: String,
        isFileLinksChanged: Boolean,
        at: Instant,
    ): Boolean {
        require(type == GroupRecordType.TIPTAP) { "레거시 기록은 수정할 수 없습니다." }
        require(!isDeleted) { "삭제된 기록입니다." }
        validateContent(content)
        val isChanged = this.content != content || isFileLinksChanged
        if (!isChanged) return false
        this.content = content
        updatedAt = at
        return true
    }

    // 같은 그룹의 다른 카드로 옮긴다. 아이디·작성자·본문·작성 일시는 유지한다. 같은 카드면 false를 반환한다.
    fun moveToCard(cardId: String): Boolean {
        require(cardId.isNotBlank()) { "카드 식별자가 올바르지 않습니다." }
        require(!isDeleted) { "삭제된 기록입니다." }
        if (this.cardId == cardId) return false
        this.cardId = cardId
        return true
    }

    // 포트폴리오 포함 여부를 바꾼다. 같은 값이면 false를 반환한다.
    fun changePortfolio(isInPortfolio: Boolean): Boolean {
        require(!isDeleted) { "삭제된 기록입니다." }
        if (this.isInPortfolio == isInPortfolio) return false
        this.isInPortfolio = isInPortfolio
        return true
    }

    // 소프트 삭제한다. 본문과 첨부 파일은 보존한다.
    fun delete(at: Instant) {
        require(!isDeleted) { "이미 삭제된 기록입니다." }
        deletedAt = at
    }

    // 삭제 표시를 해제한다. 이미 복구된 기록이면 false를 반환한다.
    fun restore(): Boolean {
        if (!isDeleted) return false
        deletedAt = null
        return true
    }

    companion object {
        const val CONTENT_MAX_BYTES: Int = 1024 * 1024

        private fun validateContent(content: String) {
            require(content.isNotEmpty()) { "기록 본문이 필요합니다." }
            require(content.toByteArray(Charsets.UTF_8).size <= CONTENT_MAX_BYTES) {
                "기록 본문은 ${CONTENT_MAX_BYTES}바이트 이하여야 합니다."
            }
        }
    }
}
