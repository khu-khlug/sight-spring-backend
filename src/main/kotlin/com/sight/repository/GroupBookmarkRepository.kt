package com.sight.repository

import com.sight.domain.group.GroupBookmark
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GroupBookmarkRepository : JpaRepository<GroupBookmark, Long> {
    // 요청자 행을 잠가 같은 회원의 즐겨찾기 변경이 순서대로 처리되게 한다.
    @Query(value = "SELECT id FROM khlug_members WHERE id = :memberId FOR UPDATE", nativeQuery = true)
    fun lockMemberById(@Param("memberId") memberId: Long): Long?

    fun existsByMemberIdAndGroupId(
        memberId: Long,
        groupId: Long,
    ): Boolean

    @Modifying
    @Query("DELETE FROM GroupBookmark b WHERE b.memberId = :memberId AND b.groupId = :groupId")
    fun deleteByMemberIdAndGroupId(
        @Param("memberId") memberId: Long,
        @Param("groupId") groupId: Long,
    ): Int
}
