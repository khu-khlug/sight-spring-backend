package com.sight.service

import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.repository.GroupLogRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class GroupLogListItem(
    val id: Long,
    val memberId: Long,
    val message: String,
    val createdAt: LocalDateTime,
)

data class GroupLogListResult(
    val count: Long,
    val logs: List<GroupLogListItem>,
)

@Service
class GroupLogService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupLogRepository: GroupLogRepository,
) {
    @Transactional(readOnly = true)
    fun listGroupLogs(
        groupId: Long,
        requesterId: Long,
        offset: Int,
        limit: Int,
    ): GroupLogListResult {
        if (!groupRepository.existsById(groupId)) {
            throw NotFoundException("그룹을 찾을 수 없습니다")
        }

        val isMember = groupMemberRepository.existsByGroupIdAndMemberId(groupId, requesterId)
        if (!isMember) {
            throw ForbiddenException("해당 그룹의 활동 로그를 조회할 권한이 없습니다")
        }

        val logs = groupLogRepository.findLogsByGroupId(groupId, offset, limit)
        val count = groupLogRepository.countLogsByGroupId(groupId)

        return GroupLogListResult(
            count = count,
            logs =
                logs.map {
                    GroupLogListItem(
                        id = it.id,
                        memberId = it.memberId,
                        message = it.message,
                        createdAt = it.createdAt,
                    )
                },
        )
    }
}
