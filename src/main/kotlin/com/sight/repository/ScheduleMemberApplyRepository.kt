package com.sight.repository

import com.sight.domain.schedule.ScheduleMemberApply
import com.sight.domain.schedule.ScheduleMemberApplyId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ScheduleMemberApplyRepository : JpaRepository<ScheduleMemberApply, ScheduleMemberApplyId> {
    fun findByScheduleIdOrderByCreatedAtAsc(scheduleId: Long): List<ScheduleMemberApply>

    fun existsByMemberIdAndScheduleId(
        memberId: Long,
        scheduleId: Long,
    ): Boolean

    fun findByMemberIdAndScheduleId(
        memberId: Long,
        scheduleId: Long,
    ): ScheduleMemberApply?

    fun findByMemberIdInAndScheduleId(
        memberIds: List<Long>,
        scheduleId: Long,
    ): List<ScheduleMemberApply>
}
