package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.repository.GroupCardRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRecordListRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class GroupRecordListResult(val offset: Long, val totalCount: Long, val records: List<GroupRecordResult>)

@Service
class GroupRecordListService(
    private val cards: GroupCardRepository,
    private val members: GroupMemberRepository,
    private val records: GroupRecordListRepository,
    private val reader: GroupRecordReadService,
) {
    @Transactional(readOnly = true)
    fun listGroupRecords(
        groupId: Long,
        cardId: Long,
        memberId: Long,
        offset: Long,
        limit: Int,
        includeDeleted: Boolean,
    ): GroupRecordListResult {
        require(offset >= 0 && limit in 1..100) { "조회 범위가 올바르지 않습니다." }
        val card = cards.findById(cardId).orElseThrow { NotFoundException("카드를 찾을 수 없습니다.") }
        if (card.groupId != groupId) throw NotFoundException("카드를 찾을 수 없습니다.")
        if (card.isDeleted) throw BadRequestException("삭제된 카드입니다.")
        if (includeDeleted && !members.existsByGroupIdAndMemberId(groupId, memberId)) {
            throw ForbiddenException("그룹원만 삭제된 기록을 조회할 수 있습니다.")
        }
        val totalCount = records.countByCardId(cardId, includeDeleted)
        if (offset >= totalCount) return GroupRecordListResult(offset, totalCount, emptyList())
        val page = records.findByCardIdOrderByRecordSort(cardId, includeDeleted, card.recordSort, offset, limit)
        return GroupRecordListResult(offset, totalCount, reader.listRecordResults(page, groupId, memberId))
    }
}
