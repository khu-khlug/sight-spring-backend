package com.sight.service

import com.sight.domain.group.GroupLogData
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GroupRecordRestoreService(private val support: GroupRecordMutationSupport) {
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
}
