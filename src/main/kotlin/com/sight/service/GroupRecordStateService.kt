package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.group.GroupLogData
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
        support.recordActivity(context, memberId, GroupLogData.RecordDeleted(record.id, card.id, card.title), now)
        support.givePointAfterCommit(record.authorMemberId, -2, "그룹 기록 삭제")
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
        support.recordActivity(context, memberId, GroupLogData.RecordRestored(record.id, card.id, card.title), now)
        support.givePointAfterCommit(memberId, 2, "그룹 기록 복구")
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
        isInPortfolio: Boolean,
    ) {
        val context = support.getLockedRecordContext(groupId, memberId, recordId, allowStopped = false)
        val (_, card, record) = context
        support.requireEditableCard(card)
        if (record.isDeleted) throw BadRequestException("삭제된 기록입니다.")
        if (!record.changePortfolio(isInPortfolio)) return
        val data =
            if (isInPortfolio) {
                GroupLogData.RecordPortfolioAdded(record.id, card.id, card.title)
            } else {
                GroupLogData.RecordPortfolioRemoved(record.id, card.id, card.title)
            }
        logs.createLog(groupId, memberId, data)
    }
}
