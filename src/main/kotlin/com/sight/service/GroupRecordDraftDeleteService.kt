package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.repository.GroupRecordDraftRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupRecordDraftDeleteService(
    private val drafts: GroupRecordDraftRepository,
    private val groups: GroupRepository,
    private val members: MemberRepository,
    private val attachments: GroupRecordAttachmentService,
) {
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
