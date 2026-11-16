package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.Group
import com.sight.domain.group.GroupCard
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupRecord
import com.sight.domain.group.GroupStatus
import com.sight.repository.GroupCardRepository
import com.sight.repository.GroupRecordRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant

data class GroupRecordMutationContext(val group: Group, val card: GroupCard, val record: GroupRecord)

@Service
class GroupRecordMutationSupport(
    private val members: MemberRepository,
    private val groups: GroupRepository,
    private val records: GroupRecordRepository,
    private val cards: GroupCardRepository,
    private val logs: GroupLogService,
    private val points: PointService,
) {
    @Transactional
    fun getLockedRecordContext(
        groupId: Long,
        memberId: Long,
        recordId: Long,
        allowStopped: Boolean = true,
    ): GroupRecordMutationContext {
        members.findForUpdateById(memberId) ?: throw NotFoundException("회원을 찾을 수 없습니다.")
        val group = groups.findLockedById(groupId) ?: throw NotFoundException("그룹을 찾을 수 없습니다.")
        val record = records.findLockedById(recordId) ?: throw NotFoundException("기록을 찾을 수 없습니다.")
        val card = cards.findById(record.cardId).orElseThrow { NotFoundException("카드를 찾을 수 없습니다.") }
        if (card.groupId != groupId) throw NotFoundException("기록을 찾을 수 없습니다.")
        if (group.status.isEnded || (!allowStopped && group.status == GroupStatus.STOP)) {
            throw BadRequestException("현재 그룹 상태에서는 변경할 수 없습니다.")
        }
        return GroupRecordMutationContext(group, card, record)
    }

    fun requireEditableCard(card: GroupCard) {
        try {
            card.requireEditable()
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(e.message ?: "변경할 수 없는 카드입니다.")
        }
    }

    @Transactional
    fun recordActivity(
        context: GroupRecordMutationContext,
        memberId: Long,
        data: GroupLogData,
        at: Instant,
    ) {
        context.group.resumeByActivity()
        context.group.recordActivity(at)
        logs.createLog(context.group.id, memberId, data)
    }

    @Transactional
    fun givePointAfterCommit(
        memberId: Long,
        delta: Int,
        message: String,
    ) {
        // 경험치 외부 호출은 DB 변경이 확정된 뒤에만 수행한다.
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    points.givePoint(memberId, delta, message)
                }
            },
        )
    }
}
