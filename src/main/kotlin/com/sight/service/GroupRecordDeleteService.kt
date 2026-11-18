package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.group.GroupLogData
import com.sight.repository.GroupRecordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GroupRecordDeleteService(
    private val support: GroupRecordMutationSupport,
    private val records: GroupRecordRepository,
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
}
