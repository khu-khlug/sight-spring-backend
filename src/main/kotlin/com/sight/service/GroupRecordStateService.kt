package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.expoint.ExpointRule
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupLogExpointChange
import com.sight.domain.group.GroupLogType
import com.sight.repository.GroupCardRepository
import com.sight.repository.GroupRecordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GroupRecordStateService(
    private val support: GroupRecordMutationSupport,
    private val records: GroupRecordRepository,
    private val cards: GroupCardRepository,
    private val logs: GroupLogService,
) {
    @Transactional
    fun deleteGroupRecord(
        groupId: Long,
        memberId: Long,
        recordId: Long,
    ) {
        val context = support.getLockedRecordContext(groupId, memberId, recordId)
        val (group, card, record) = context
        support.requireEditableCard(card)
        if (record.isDeleted) throw BadRequestException("이미 삭제된 기록입니다.")
        val now = Instant.now()
        records.delete(record, now)
        card.decreaseRecordCount(now)
        group.decreaseRecordCount()
        group.resumeByActivity()
        group.recordActivity(now)
        logs.revertExpointChange(
            groupId = groupId,
            actorMemberId = memberId,
            targetId = record.id.toString(),
            targetLogTypes = setOf(GroupLogType.RECORD_CREATED, GroupLogType.RECORD_RESTORED),
            revertLog = GroupLogData.RecordDeleted(record.id, card.id, card.title),
            message = "그룹 기록 삭제",
            fallbackRule = ExpointRule.기록삭제,
            fallbackMemberIds = listOf(record.authorMemberId),
        )
    }

    @Transactional
    fun updateGroupRecordRestored(
        groupId: Long,
        memberId: Long,
        recordId: Long,
    ) {
        val context = support.getLockedRecordContext(groupId, memberId, recordId)
        val (group, card, record) = context
        support.requireEditableCard(card)
        if (!record.restore()) return
        val now = Instant.now()
        card.increaseRecordCount(now)
        group.increaseRecordCount()
        val point = ExpointRule.기록복구.point
        support.recordActivity(
            context,
            memberId,
            GroupLogData.RecordRestored(record.id, card.id, card.title),
            now,
            GroupLogExpointChange(targetId = record.id.toString(), point = point, memberIds = listOf(memberId)),
        )
        support.givePointAfterCommit(memberId, point, "그룹 기록 복구")
    }

    @Transactional
    fun updateGroupRecordPosition(
        groupId: Long,
        memberId: Long,
        recordId: Long,
        targetCardId: Long,
    ) {
        val context = support.getLockedRecordContext(groupId, memberId, recordId)
        val (_, card, record) = context
        support.requireEditableCard(card)
        if (record.isDeleted) throw BadRequestException("삭제된 기록입니다.")
        val target = cards.findById(targetCardId).orElseThrow { BadRequestException("목적지 카드를 찾을 수 없습니다.") }
        if (target.groupId != groupId) throw BadRequestException("목적지 카드는 같은 그룹에 속해야 합니다.")
        support.requireEditableCard(target)
        if (!record.moveToCard(target.id)) return
        val now = Instant.now()
        card.decreaseRecordCount(now)
        target.increaseRecordCount(now)
        support.recordActivity(context, memberId, GroupLogData.RecordMoved(record.id, card.id, card.title, target.id, target.title), now)
    }

    @Transactional
    fun updateGroupRecordPortfolio(
        groupId: Long,
        memberId: Long,
        recordId: Long,
        requestedPortfolioState: Boolean,
    ) {
        val context = support.getLockedRecordContext(groupId, memberId, recordId, allowStopped = false)
        val (_, card, record) = context
        support.requireEditableCard(card)
        if (record.isDeleted) throw BadRequestException("삭제된 기록입니다.")
        if (!record.changePortfolio(requestedPortfolioState)) return
        val data =
            if (requestedPortfolioState) {
                GroupLogData.RecordPortfolioAdded(record.id, card.id, card.title)
            } else {
                GroupLogData.RecordPortfolioRemoved(record.id, card.id, card.title)
            }
        logs.createLog(groupId, memberId, data)
    }
}
