package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
// 유니크(group_id, seminar_id): 같은 세미나에 한 번만 제출, 그룹별 활동보고 조회.
@Table(
    name = "khlug_group_activity_report",
    uniqueConstraints = [UniqueConstraint(columnNames = ["group_id", "seminar_id"])],
)
class GroupActivityReport(
    id: String,
    groupId: Long,
    seminarId: String,
    isPresentation: Boolean,
    createdAt: Instant,
) {
    // 활동보고 식별자.
    @Id
    @Column(name = "id", nullable = false, length = ID_MAX_LENGTH)
    val id: String = id

    // 제출한 그룹. 세미나와 함께 유일해 같은 세미나에 한 번만 제출한다.
    @Column(name = "group_id", nullable = false)
    val groupId: Long = groupId

    // 제출 대상 세미나.
    @Column(name = "seminar_id", nullable = false, length = 100)
    val seminarId: String = seminarId

    // true면 세미나 발표, false면 보고서.
    @Column(name = "is_presentation", nullable = false)
    var isPresentation: Boolean = isPresentation
        private set

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = createdAt

    // 유형이나 파일이 마지막으로 바뀐 일시.
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = createdAt
        private set

    init {
        require(id.isNotBlank() && id.length <= ID_MAX_LENGTH) { "활동보고 식별자가 올바르지 않습니다." }
        require(groupId > 0) { "그룹 식별자가 올바르지 않습니다." }
        require(seminarId.isNotBlank()) { "세미나 식별자가 올바르지 않습니다." }
    }

    fun update(
        isPresentation: Boolean,
        updatedAt: Instant,
    ) {
        this.isPresentation = isPresentation
        this.updatedAt = updatedAt
    }

    companion object {
        const val ID_MAX_LENGTH: Int = 100
    }
}
