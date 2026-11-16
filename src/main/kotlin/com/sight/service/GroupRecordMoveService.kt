package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.group.GroupLogData
import com.sight.repository.GroupCardRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GroupRecordMoveService(private val support: GroupRecordMutationSupport, private val cards: GroupCardRepository) {
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
}
