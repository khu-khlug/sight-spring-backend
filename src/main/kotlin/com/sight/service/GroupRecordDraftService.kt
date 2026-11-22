package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ConflictException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupRecordDraft
import com.sight.domain.group.GroupRecordType
import com.sight.repository.GroupCardRepository
import com.sight.repository.GroupRecordDraftRepository
import com.sight.repository.GroupRecordRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
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
class GroupRecordDraftService(
    private val drafts: GroupRecordDraftRepository,
    private val cards: GroupCardRepository,
    private val attachments: GroupRecordAttachmentService,
    private val groups: GroupRepository,
    private val records: GroupRecordRepository,
    private val members: MemberRepository,
    private val ids: IntegerIdService,
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

    @Transactional
    fun updateGroupRecordDraft(
        groupId: Long,
        memberId: Long,
        cardId: Long,
        recordId: Long?,
        content: String,
        updatedAt: Instant,
        files: List<RecordFileInput>,
    ): List<RecordFileUploadResult> {
        // 슬롯이 비어 있는 최초 저장도 회원 행으로 직렬화한다.
        members.findForUpdateById(memberId) ?: throw NotFoundException("회원을 찾을 수 없습니다.")
        val draft = drafts.findForUpdateByMemberId(memberId)
        // 대상 전환은 이전 그룹의 파일 연결도 해제하므로 두 그룹을 같은 순서로 잠근다.
        val lockedGroups =
            listOfNotNull(groupId, draft?.groupId).distinct().sorted()
                .associateWith { groups.findLockedById(it) }
        val group = lockedGroups[groupId] ?: throw NotFoundException("그룹을 찾을 수 없습니다.")
        if (group.status.isEnded) throw BadRequestException("종료된 그룹입니다.")
        if (draft?.isNewerThan(updatedAt) == true) throw ConflictException("서버에 더 최신 임시저장이 있습니다.")
        val card = cards.findById(cardId).orElseThrow { BadRequestException("카드를 찾을 수 없습니다.") }
        if (card.groupId != groupId) throw BadRequestException("이 그룹의 카드가 아닙니다.")
        if (card.isDeleted) throw BadRequestException("삭제된 카드에는 임시저장할 수 없습니다.")
        if (recordId != null) {
            val record =
                records.findByIdAndDeletedAtIsNull(recordId) ?: run {
                    if (records.existsDeletedByIdAndGroupId(recordId, groupId)) {
                        throw BadRequestException("임시저장 대상이 올바르지 않습니다.")
                    }
                    throw BadRequestException("기록을 찾을 수 없습니다.")
                }
            if (record.cardId != cardId || record.type != GroupRecordType.TIPTAP) {
                throw BadRequestException("임시저장 대상이 올바르지 않습니다.")
            }
            if (!record.isAuthor(memberId)) throw ForbiddenException("기록 작성자만 수정 내용을 임시저장할 수 있습니다.")
        }
        val reset = draft != null && !draft.isSameTarget(groupId, cardId, recordId)
        val saved =
            try {
                if (draft == null) {
                    GroupRecordDraft(ids.nextId(), memberId, groupId, cardId, recordId, content, updatedAt)
                } else {
                    draft.overwrite(groupId, cardId, recordId, content, updatedAt)
                    draft
                }
            } catch (e: IllegalArgumentException) {
                throw BadRequestException(requireNotNull(e.message))
            }
        if (draft == null) drafts.save(saved)
        return attachments.replaceDraftFiles(groupId, memberId, saved.id, files, reset)
    }

    @Transactional
    fun deleteGroupRecordDraft(
        groupId: Long,
        memberId: Long,
    ) {
        members.findForUpdateById(memberId) ?: throw NotFoundException("회원을 찾을 수 없습니다.")
        val group = groups.findLockedById(groupId) ?: throw NotFoundException("그룹을 찾을 수 없습니다.")
        if (group.status.isEnded) throw BadRequestException("종료된 그룹입니다.")
        val draft = drafts.findByMemberId(memberId)?.takeIf { it.belongsTo(groupId) } ?: return
        attachments.deleteDraftFiles(draft.id)
        drafts.delete(draft)
    }
}
