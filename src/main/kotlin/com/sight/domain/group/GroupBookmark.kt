package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
// 유니크(member_id, group_id): 중복 즐겨찾기 방지, 회원별 즐겨찾기 조회.
@Table(
    name = "khlug_group_bookmark",
    uniqueConstraints = [UniqueConstraint(columnNames = ["member_id", "group_id"])],
)
class GroupBookmark(
    id: Long,
    memberId: Long,
    groupId: Long,
    createdAt: Instant,
) {
    // 즐겨찾기 행의 식별자.
    @Id
    @Column(name = "id", nullable = false)
    val id: Long = id

    // 즐겨찾기한 회원. 그룹과 함께 유일해 동시 요청에도 한 번만 추가된다.
    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    // 즐겨찾기한 그룹.
    @Column(name = "group_id", nullable = false)
    val groupId: Long = groupId

    // 즐겨찾기 추가 일시.
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = createdAt

    init {
        require(id > 0) { "식별자가 올바르지 않습니다." }
        require(memberId > 0) { "회원 식별자가 올바르지 않습니다." }
        require(groupId > 0) { "그룹 식별자가 올바르지 않습니다." }
    }
}
