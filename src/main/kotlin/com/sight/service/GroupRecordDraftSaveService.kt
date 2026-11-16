package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ConflictException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupRecord
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

@Service
class GroupRecordDraftSaveService(
    private val drafts: GroupRecordDraftRepository,
    private val groups: GroupRepository,
    private val cards: GroupCardRepository,
    private val records: GroupRecordRepository,
    private val members: MemberRepository,
    private val attachments: GroupRecordAttachmentService,
    private val ids: IntegerIdService,
) {
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
        val card = cards.findById(cardId).orElseThrow { BadRequestException("이 그룹의 카드가 아닙니다.") }
        if (card.groupId != groupId || card.isDeleted) throw BadRequestException("임시저장 대상이 올바르지 않습니다.")
        if (recordId != null) {
            val record = records.findById(recordId).orElseThrow { BadRequestException("기록을 찾을 수 없습니다.") }
            if (record.cardId != cardId || record.isDeleted || record.type != GroupRecordType.TIPTAP) {
                throw BadRequestException("임시저장 대상이 올바르지 않습니다.")
            }
            if (!record.isAuthor(memberId)) throw ForbiddenException("기록 작성자만 수정 내용을 임시저장할 수 있습니다.")
        }
        if (content.toByteArray(Charsets.UTF_8).size > GroupRecord.CONTENT_MAX_BYTES) {
            throw BadRequestException("임시저장 본문은 1 MiB 이하여야 합니다.")
        }
        val reset = draft != null && !draft.isSameTarget(groupId, cardId, recordId)
        val saved =
            if (draft == null) {
                drafts.save(GroupRecordDraft(ids.nextId(), memberId, groupId, cardId, recordId, content, updatedAt))
            } else {
                draft.overwrite(groupId, cardId, recordId, content, updatedAt)
                draft
            }
        return attachments.replaceDraftFiles(groupId, memberId, saved.id, files, reset).uploads
    }
}
