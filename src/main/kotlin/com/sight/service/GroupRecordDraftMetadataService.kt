package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.repository.GroupCardRepository
import com.sight.repository.GroupRecordDraftRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class GroupRecordDraftMetadataResult(
    val isNewRecord: Boolean,
    val cardId: Long,
    val cardTitle: String,
    val recordId: Long?,
    val updatedAt: Instant,
)

@Service
class GroupRecordDraftMetadataService(
    private val drafts: GroupRecordDraftRepository,
    private val cards: GroupCardRepository,
) {
    @Transactional(readOnly = true)
    fun getGroupRecordDraftMetadataByMemberId(
        groupId: Long,
        memberId: Long,
    ): GroupRecordDraftMetadataResult? {
        val draft = drafts.findByMemberId(memberId)?.takeIf { it.belongsTo(groupId) } ?: return null
        val card = cards.findById(draft.cardId).orElseThrow { NotFoundException("카드를 찾을 수 없습니다.") }
        return GroupRecordDraftMetadataResult(draft.isNewRecord, draft.cardId, card.title, draft.recordId, draft.updatedAt)
    }
}
