package com.sight.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.core.exception.NotFoundException
import com.sight.domain.expoint.ExpointRule
import com.sight.domain.group.GroupExpointChange
import com.sight.domain.group.GroupLog
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupLogExpointChange
import com.sight.domain.group.GroupLogType
import com.sight.repository.GroupExpointChangeRepository
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
    private val groupExpointChangeRepository: GroupExpointChangeRepository,
    private val memberRepository: MemberRepository,
    private val integerIdService: IntegerIdService,
    private val objectMapper: ObjectMapper,
    private val pointService: PointService,
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
    // 행위로 경험치를 지급·차감했다면 expointChange에 적용한 내역을 함께 남긴다. 경험치 지급·차감 자체는 호출한 쪽이 한다.
    @Transactional
    fun createLog(
        groupId: Long,
        actorMemberId: Long,
        data: GroupLogData,
        expointChange: GroupLogExpointChange? = null,
    ) {
        save(groupId, actorMemberId, data, expointChange)
    }

    // 회원이 아닌 시스템이 행위자인 로그를 남긴다. 조회 시 행위자는 null이다.
    // 행위로 경험치를 지급·차감했다면 expointChange에 적용한 내역을 함께 남긴다. 경험치 지급·차감 자체는 호출한 쪽이 한다.
    @Transactional
    fun createSystemLog(
        groupId: Long,
        data: GroupLogData,
        expointChange: GroupLogExpointChange? = null,
    ) {
        save(groupId, null, data, expointChange)
    }

    // targetId로 기록된 변동 중 로그타입이 targetLogTypes에 속하는 가장 최근 행위의 변동을 같은 회원에게 같은 양만큼 반대로 적용하고,
    // 되돌린 행위를 revertLog로 남기며 적용한 변동을 같은 targetId로 기록한다. actorMemberId가 null이면 시스템 로그로 남긴다.
    // 해당하는 변동이 없으면 현재 규칙 fallbackRule을 fallbackMemberIds에게 적용한다.
    // 적용한 변동을 반환하며, 적용할 회원이 없으면 경험치를 바꾸지 않고 빈 목록을 반환한다.
    // 같은 행위를 두 번 되돌리지 않는 것은 호출한 쪽의 상태 검사가 보장한다. 사용법은 docs/plans/group/global.md의 경험치 절을 따른다.
    @Transactional
    fun revertExpointChange(
        groupId: Long,
        actorMemberId: Long?,
        targetId: String,
        targetLogTypes: Set<GroupLogType>,
        revertLog: GroupLogData,
        message: String,
        fallbackRule: ExpointRule,
        fallbackMemberIds: List<Long>,
    ): List<GroupExpointChange> {
        require(targetLogTypes.isNotEmpty()) { "되돌릴 로그타입이 필요합니다." }
        val recorded =
            groupExpointChangeRepository.findLatestGroupLogIdByTargetIdAndLogTypes(targetId, targetLogTypes)
                ?.let { groupExpointChangeRepository.findByGroupLogId(it) }
                .orEmpty()
        val pointByMemberId =
            if (recorded.isNotEmpty()) {
                recorded.associate { it.memberId to -it.point }
            } else {
                fallbackMemberIds.distinct().associateWith { fallbackRule.point }
            }

        val logId = save(groupId, actorMemberId, revertLog)
        val applied =
            pointByMemberId.map { (memberId, point) ->
                GroupExpointChange(
                    id = integerIdService.nextId(),
                    groupLogId = logId,
                    targetId = targetId,
                    memberId = memberId,
                    point = point,
                )
            }
        groupExpointChangeRepository.saveAll(applied)
        applied.forEach { pointService.givePoint(targetUserId = it.memberId, point = it.point, message = message) }
        return applied
    }

    private fun save(
        groupId: Long,
        actorMemberId: Long?,
        data: GroupLogData,
        expointChange: GroupLogExpointChange? = null,
    ): Long {
        val logId = integerIdService.nextId()
        groupLogRepository.save(
            GroupLog(
                id = logId,
                groupId = groupId,
                actorMemberId = actorMemberId,
                logType = data.logType,
                logData = objectMapper.writeValueAsString(data.fields()),
                createdAt = Instant.now(),
            ),
        )
        if (expointChange != null) {
            groupExpointChangeRepository.saveAll(
                expointChange.memberIds.map { memberId ->
                    GroupExpointChange(
                        id = integerIdService.nextId(),
                        groupLogId = logId,
                        targetId = expointChange.targetId,
                        memberId = memberId,
                        point = expointChange.point,
                    )
                },
            )
        }
        return logId
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
