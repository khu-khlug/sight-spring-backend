package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant

@Entity
@Table(
    name = "khlug_group_list",
    indexes = [Index(columnList = "group_id, position, id")],
)
class GroupList(
    id: String,
    groupId: Long,
    title: String,
    position: Int,
    createdAt: Instant,
) {
    // 리스트 식별자. 그룹 API 아이디 기준에 따라 문자열(ULID)이다.
    @Id
    @Column(name = "id", nullable = false, length = ID_MAX_LENGTH)
    val id: String = id

    // 소속 그룹.
    @Column(name = "group_id", nullable = false)
    val groupId: Long = groupId

    // 리스트 이름. 앞뒤 공백을 제거해 저장한다.
    @Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
    var title: String = title
        private set

    // 리스트 설명. 일반 텍스트이며 없으면 빈 문자열이다.
    @Column(name = "description", nullable = false, length = DESCRIPTION_MAX_LENGTH)
    var description: String = ""
        private set

    // 그룹 안의 표시 순서. 오름차순으로 보여 주며, 같으면 리스트 아이디 오름차순이다.
    @Column(name = "position", nullable = false)
    var position: Int = position
        private set

    // 삭제되지 않은 카드 수. 비활성 카드를 포함한다.
    @Column(name = "card_count", nullable = false)
    var cardCount: Long = 0
        private set

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = createdAt

    // 이름·설명을 마지막으로 바꾼 일시.
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = createdAt
        private set

    // JPA 낙관적 잠금용 수정 버전. 순서·카드 수의 동시 변경 충돌을 저장 시 감지한다.
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        private set

    init {
        require(id.isNotBlank() && id.length <= ID_MAX_LENGTH) { "리스트 식별자가 올바르지 않습니다." }
        require(groupId > 0) { "그룹 식별자가 올바르지 않습니다." }
        this.title = normalizeTitle(title)
    }

    // 보낸 항목만 바꾼다. 실제로 바뀌었으면 true를 반환한다.
    fun update(title: String?, description: String?, at: Instant): Boolean {
        require(title != null || description != null) { "수정할 항목이 필요합니다." }
        val newTitle = title?.let { normalizeTitle(it) } ?: this.title
        val newDescription = description ?: this.description
        require(newDescription.codePointCount(0, newDescription.length) <= DESCRIPTION_MAX_LENGTH) {
            "리스트 설명은 ${DESCRIPTION_MAX_LENGTH}자 이하여야 합니다."
        }
        if (newTitle == this.title && newDescription == this.description) return false
        this.title = newTitle
        this.description = newDescription
        updatedAt = at
        return true
    }

    // 표시 순서를 바꾼다. 순서 계산은 같은 그룹의 다른 리스트와 함께 해야 하므로 계산된 값을 받는다.
    fun moveTo(position: Int) {
        this.position = position
    }

    fun increaseCardCount() {
        cardCount += 1
    }

    fun decreaseCardCount() {
        require(cardCount > 0) { "카드 수는 음수가 될 수 없습니다." }
        cardCount -= 1
    }

    companion object {
        const val ID_MAX_LENGTH: Int = 100
        const val TITLE_MAX_LENGTH: Int = 255
        const val DESCRIPTION_MAX_LENGTH: Int = 1000

        private fun normalizeTitle(title: String): String {
            val trimmed = title.trim()
            require(trimmed.isNotEmpty()) { "리스트 이름이 필요합니다." }
            require(trimmed.codePointCount(0, trimmed.length) <= TITLE_MAX_LENGTH) {
                "리스트 이름은 ${TITLE_MAX_LENGTH}자 이하여야 합니다."
            }
            return trimmed
        }
    }
}
