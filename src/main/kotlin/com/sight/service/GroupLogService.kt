package com.sight.service

import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.InternalServerErrorException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupLog
import com.sight.repository.GroupLogRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import kotlin.random.Random

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

        val logs = groupRepository.findGroupLogsByGroupId(groupId, offset, limit)
        val count = groupRepository.countGroupLogsByGroupId(groupId)

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

    @Transactional
    fun createLog(
        groupId: Long,
        memberId: Long,
        message: String,
    ) {
        repeat(MAX_GROUP_LOG_ID_RETRY + 1) {
            val id = createNewGroupLogId()
            if (!groupLogRepository.existsById(id)) {
                groupLogRepository.save(
                    GroupLog(id = id, group = groupId, member = memberId, message = message),
                )
                return
            }
        }
        throw InternalServerErrorException(
            "group_log ID 채번을 ${MAX_GROUP_LOG_ID_RETRY}회 재시도한 후에도 빈 ID를 찾지 못했습니다",
        )
    }

    private fun createNewGroupLogId(): Long {
        val minimumId = 1_000_000

        val millisUntil20250101 =
            LocalDateTime.of(
                2025,
                Month.JANUARY,
                1,
                0,
                0,
                0,
            ).atZone(KST).toInstant().toEpochMilli()
        val currentTimestamp = System.currentTimeMillis()

        val timePart = (currentTimestamp - millisUntil20250101) / 1000 / 60

        val randomPart = Random.nextLong(0L, 1000L)

        return minimumId + timePart * 1000 + randomPart
    }

    companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
        private const val MAX_GROUP_LOG_ID_RETRY = 3
    }
}
