package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupRecord
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
class GroupRecordUpdateService(
    private val groups: GroupRepository,
    private val cards: GroupCardRepository,
    private val records: GroupRecordRepository,
    private val members: MemberRepository,
    private val drafts: GroupRecordDraftRepository,
    private val attachments: GroupRecordAttachmentService,
    private val logs: GroupLogService,
) {
    @Transactional
    fun updateGroupRecord(
        groupId: Long,
        recordId: Long,
        memberId: Long,
        content: String,
        files: List<RecordFileInput>,
    ): List<RecordFileUploadResult> {
        members.findForUpdateById(memberId) ?: throw NotFoundException("회원을 찾을 수 없습니다.")
        val group = groups.findLockedById(groupId) ?: throw NotFoundException("그룹을 찾을 수 없습니다.")
        val record = records.findLockedById(recordId) ?: throw NotFoundException("기록을 찾을 수 없습니다.")
        val card = cards.findById(record.cardId).orElseThrow { NotFoundException("카드를 찾을 수 없습니다.") }
        if (card.groupId != groupId) throw NotFoundException("기록을 찾을 수 없습니다.")
        if (!record.isAuthor(memberId)) throw ForbiddenException("기록 작성자만 수정할 수 있습니다.")
        if (group.status.isEnded) throw BadRequestException("종료된 그룹입니다.")
        if (card.isDeleted || card.isDisabled || record.isDeleted) throw BadRequestException("삭제되거나 비활성 상태인 카드·기록입니다.")
        if (record.type != GroupRecordType.TIPTAP) throw BadRequestException("레거시 기록은 수정할 수 없습니다.")
        if (content.isEmpty() || content.toByteArray(Charsets.UTF_8).size > GroupRecord.CONTENT_MAX_BYTES) {
            throw BadRequestException("기록 본문은 비어 있지 않은 1 MiB 이하 문자열이어야 합니다.")
        }
        val now = Instant.now()
        val change = attachments.replaceRecordFiles(groupId, memberId, recordId, files)
        val changed = record.edit(content, change.isChanged, now)
        val draft = drafts.findByMemberId(memberId)
        if (draft != null && draft.isSameTarget(groupId, card.id, recordId)) {
            attachments.deleteDraftFiles(draft.id)
            drafts.delete(draft)
        }
        if (changed) {
            group.resumeByActivity()
            group.recordActivity(now)
            logs.createLog(groupId, memberId, GroupLogData.RecordUpdated(recordId, card.id, card.title))
        }
        return change.uploads
    }
}
