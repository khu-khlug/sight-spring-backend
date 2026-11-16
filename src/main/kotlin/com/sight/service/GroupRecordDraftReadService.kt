package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.repository.GroupCardRepository
import com.sight.repository.GroupRecordDraftRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class GroupRecordDraftResult(
    val isNewRecord: Boolean,
    val cardId: Long,
    val cardTitle: String,
    val recordId: Long?,
    val content: String,
    val updatedAt: Instant,
    val files: List<RecordFileResult>,
)

@Service
class GroupRecordDraftReadService(
    private val drafts: GroupRecordDraftRepository,
    private val cards: GroupCardRepository,
    private val attachments: GroupRecordAttachmentService,
) {
    @Transactional(readOnly = true)
    fun getGroupRecordDraftByMemberId(
        groupId: Long,
        memberId: Long,
    ): GroupRecordDraftResult {
        val draft =
            drafts.findByMemberId(memberId)?.takeIf { it.belongsTo(groupId) }
                ?: throw NotFoundException("임시저장을 찾을 수 없습니다.")
        val card = cards.findById(draft.cardId).orElseThrow { NotFoundException("카드를 찾을 수 없습니다.") }
        return GroupRecordDraftResult(
            draft.isNewRecord,
            draft.cardId,
            card.title,
            draft.recordId,
            draft.content,
            draft.updatedAt,
            attachments.getDraftFiles(groupId, draft.id),
        )
    }
}
