package com.sight.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupLog
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupLogType
import com.sight.repository.GroupLogRepository
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
    val page: Int,
    val totalCount: Long,
    val receivedCount: Int,
    val logs: List<GroupLogListItem>,
)

@Service
class GroupLogService(
    private val groupRepository: GroupRepository,
    private val groupLogRepository: GroupLogRepository,
    private val memberRepository: MemberRepository,
    private val integerIdService: IntegerIdService,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(readOnly = true)
    fun listGroupLogs(
        groupId: Long,
        page: Int,
    ): GroupLogListResult {
        if (!groupRepository.existsById(groupId)) {
            throw NotFoundException("그룹을 찾을 수 없습니다")
        }

        val offset = page.toLong() * PAGE_SIZE
        val logs = groupRepository.findGroupLogsByGroupIdOrderByLatest(groupId, offset = offset, limit = PAGE_SIZE)
        val totalCount = groupRepository.countGroupLogsByGroupId(groupId)
        // 탈퇴해도 회원 행과 실명은 남으므로 행위자 이름은 조회 시점의 회원 실명을 쓴다.
        val actorNames =
            memberRepository.findAllById(logs.mapNotNull { it.actorMemberId }.distinct())
                .associate { it.id to it.realname }

        return GroupLogListResult(
            page = page,
            totalCount = totalCount,
            receivedCount = logs.size,
            logs = logs.map { it.toListItem(actorNames) },
        )
    }

    // 회원이 행위자인 로그를 남긴다.
    @Transactional
    fun createLog(
        groupId: Long,
        actorMemberId: Long,
        data: GroupLogData,
    ) {
        save(groupId, actorMemberId, data)
    }

    // 회원이 아닌 시스템이 행위자인 로그를 남긴다. 조회 시 행위자는 null이다.
    @Transactional
    fun createSystemLog(
        groupId: Long,
        data: GroupLogData,
    ) {
        save(groupId, null, data)
    }

    private fun save(
        groupId: Long,
        actorMemberId: Long?,
        data: GroupLogData,
    ) {
        groupLogRepository.save(
            GroupLog(
                id = integerIdService.nextId(),
                groupId = groupId,
                actorMemberId = actorMemberId,
                logType = data.logType,
                logData = objectMapper.writeValueAsString(data.fields()),
                createdAt = Instant.now(),
            ),
        )
    }

    private fun GroupLog.toListItem(actorNames: Map<Long, String>): GroupLogListItem =
        GroupLogListItem(
            id = id,
            actor = actorMemberId?.let { GroupLogActor(userId = it, name = actorNames[it].orEmpty()) },
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
