package com.sight.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupLog
import com.sight.domain.group.GroupLogType
import com.sight.repository.GroupLogRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class GroupLogActor(
    val userId: Long,
    val name: String,
)

data class GroupLogListItem(
    val id: Long,
    val actor: GroupLogActor?,
    val logType: GroupLogType,
    val logData: Map<String, Any?>,
    val createdAt: Instant,
)

data class GroupLogListResult(
    val skip: Int,
    val totalCount: Long,
    val receivedCount: Int,
    val logs: List<GroupLogListItem>,
)

@Service
class GroupLogService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupLogRepository: GroupLogRepository,
    private val memberRepository: MemberRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(readOnly = true)
    fun listGroupLogs(
        groupId: Long,
        requesterId: Long,
        skip: Int,
    ): GroupLogListResult {
        if (!groupRepository.existsById(groupId)) {
            throw NotFoundException("그룹을 찾을 수 없습니다")
        }

        val isMember = groupMemberRepository.existsByGroupIdAndMemberId(groupId, requesterId)
        if (!isMember) {
            throw ForbiddenException("해당 그룹의 활동 로그를 조회할 권한이 없습니다")
        }

        val logs = groupRepository.findGroupLogsByGroupIdOrderByLatest(groupId, offset = skip, limit = PAGE_SIZE)
        val totalCount = groupRepository.countGroupLogsByGroupId(groupId)

        return GroupLogListResult(
            skip = skip,
            totalCount = totalCount,
            receivedCount = logs.size,
            logs = logs.map { it.toListItem() },
        )
    }

    // 회원이 행위자인 로그를 남긴다. 행위자 실명은 발생 당시 값으로 저장한다.
    // logData는 로그타입의 필수 필드를 모두 가진 객체여야 한다.
    @Transactional
    fun createLog(
        groupId: Long,
        actorMemberId: Long,
        logType: GroupLogType,
        logData: Map<String, Any?>,
    ) {
        require(logType != GroupLogType.LEGACY) { "신규 로그를 LEGACY로 저장할 수 없습니다." }
        val missingFields = logType.missingFields(logData.filterValues { it != null }.keys)
        require(missingFields.isEmpty()) { "로그데이터에 필수 필드가 없습니다: $missingFields" }

        val actor =
            memberRepository.findById(actorMemberId).orElseThrow {
                NotFoundException("회원을 찾을 수 없습니다")
            }

        groupLogRepository.save(
            GroupLog(
                groupId = groupId,
                actorMemberId = actor.id,
                actorName = actor.realname,
                logType = logType,
                logData = objectMapper.writeValueAsString(logData),
                createdAt = Instant.now(),
            ),
        )
    }

    private fun GroupLog.toListItem(): GroupLogListItem =
        GroupLogListItem(
            id = id,
            actor =
                if (actorMemberId != null && actorName != null) {
                    GroupLogActor(userId = actorMemberId, name = actorName)
                } else {
                    null
                },
            logType = logType,
            logData = objectMapper.readValue(logData, LOG_DATA_TYPE),
            createdAt = createdAt,
        )

    companion object {
        // 한 번에 반환하는 최대 로그 수.
        const val PAGE_SIZE: Int = 50

        private val LOG_DATA_TYPE = object : TypeReference<Map<String, Any?>>() {}
    }
}
