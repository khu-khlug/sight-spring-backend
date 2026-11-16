package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupRecord
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.GroupCardRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRecordDraftRepository
import com.sight.repository.GroupRecordRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant

data class CreateGroupRecordResult(
    val id: Long,
    val authorName: String,
    val content: String,
    val createdAt: Instant,
    val files: List<RecordFileResult>,
    val uploads: List<RecordFileUploadResult>,
)

@Service
class GroupRecordCreateService(
    private val groups: GroupRepository,
    private val cards: GroupCardRepository,
    private val records: GroupRecordRepository,
    private val members: MemberRepository,
    private val groupMembers: GroupMemberRepository,
    private val drafts: GroupRecordDraftRepository,
    private val attachments: GroupRecordAttachmentService,
    private val ids: IntegerIdService,
    private val logs: GroupLogService,
    private val notifications: NotificationService,
    private val points: PointService,
) {
    @Transactional
    fun createGroupRecord(
        groupId: Long,
        cardId: Long,
        memberId: Long,
        content: String,
        files: List<RecordFileInput>,
    ): CreateGroupRecordResult {
        val member = members.findForUpdateById(memberId) ?: throw NotFoundException("회원을 찾을 수 없습니다.")
        val group = groups.findLockedById(groupId) ?: throw NotFoundException("그룹을 찾을 수 없습니다.")
        val card = cards.findById(cardId).orElseThrow { NotFoundException("카드를 찾을 수 없습니다.") }
        if (card.groupId != groupId) throw NotFoundException("카드를 찾을 수 없습니다.")
        if (group.status.isEnded) throw BadRequestException("종료된 그룹입니다.")
        if (card.isDeleted || card.isDisabled) throw BadRequestException("삭제되거나 비활성 상태인 카드입니다.")
        if (content.isEmpty() || content.toByteArray(Charsets.UTF_8).size > GroupRecord.CONTENT_MAX_BYTES) {
            throw BadRequestException("기록 본문은 비어 있지 않은 1 MiB 이하 문자열이어야 합니다.")
        }
        val now = Instant.now()
        val record = records.save(GroupRecord(ids.nextId(), cardId, memberId, content, now))
        val change = attachments.replaceRecordFiles(groupId, memberId, record.id, files)
        val draft = drafts.findByMemberId(memberId)
        if (draft != null && draft.isSameTarget(groupId, cardId, null)) {
            attachments.deleteDraftFiles(draft.id)
            drafts.delete(draft)
        }
        card.increaseRecordCount(now)
        group.increaseRecordCount()
        group.resumeByActivity()
        group.recordActivity(now)
        logs.createLog(groupId, memberId, GroupLogData.RecordCreated(record.id, cardId, card.title))
        groupMembers.findByGroupId(groupId).forEach {
            notifications.createNotification(
                it.memberId,
                NotificationCategory.GROUP,
                "그룹 기록 작성",
                "${group.title} 그룹의 ${card.title} 카드에 기록이 작성되었습니다.",
            )
        }
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    points.givePoint(memberId, 2, "그룹 기록 작성")
                }
            },
        )
        return CreateGroupRecordResult(
            record.id,
            member.realname,
            content,
            now,
            attachments.getRecordFiles(groupId, record.id),
            change.uploads,
        )
    }
}
