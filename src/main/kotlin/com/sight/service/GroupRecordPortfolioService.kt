package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.group.GroupLogData
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupRecordPortfolioService(private val support: GroupRecordMutationSupport, private val logs: GroupLogService) {
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
