package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
// 유니크(group_id, member_id): 중복 참가 방지, 그룹별 그룹원 조회.
// 인덱스(member_id): 회원이 참가한 그룹 조회(그룹 목록 joined 필터).
@Table(
    name = "khlug_group_member",
    uniqueConstraints = [UniqueConstraint(columnNames = ["group_id", "member_id"])],
    indexes = [Index(columnList = "member_id")],
)
class GroupMember(
    groupId: Long,
    memberId: Long,
    joinedAt: Instant,
) {
    // 그룹원 관계 행의 식별자.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0

    // 소속 그룹. 회원과 함께 유일해 동시 참가 요청에도 관계가 한 번만 생긴다.
    @Column(name = "group_id", nullable = false)
    val groupId: Long = groupId

    // 그룹원인 회원.
    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    // 참가 일시.
    @Column(name = "joined_at", nullable = false)
    val joinedAt: Instant = joinedAt

    init {
        require(groupId > 0) { "그룹 식별자가 올바르지 않습니다." }
        require(memberId > 0) { "회원 식별자가 올바르지 않습니다." }
    }
}
