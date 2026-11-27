package com.sight.repository

import com.sight.domain.group.GroupMember
import com.sight.repository.dto.GroupMemberListDto
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface GroupMemberRepository : Repository<GroupMember, Long>, GroupMemberRepositoryCustom {
    fun findByGroupId(groupId: Long): List<GroupMember>

    fun findByMemberId(memberId: Long): List<GroupMember>

    fun findByGroupIdAndMemberId(
        groupId: Long,
        memberId: Long,
    ): GroupMember?

    fun existsByGroupIdAndMemberId(
        groupId: Long,
        memberId: Long,
    ): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.memberId = :memberId")
    fun findLockedByGroupIdAndMemberId(
        @Param("groupId") groupId: Long,
        @Param("memberId") memberId: Long,
    ): GroupMember?
}

interface GroupMemberRepositoryCustom {
    fun findMemberListByGroupId(groupId: Long): List<GroupMemberListDto>

    fun save(
        id: Long,
        groupId: Long,
        memberId: Long,
        joinedAt: Instant,
    )

    fun saveAll(groupMembers: List<GroupMember>)

    // 삭제한 행 수를 반환한다. 0이면 이미 그룹원이 아니다.
    fun delete(
        groupId: Long,
        memberId: Long,
    ): Int
}
