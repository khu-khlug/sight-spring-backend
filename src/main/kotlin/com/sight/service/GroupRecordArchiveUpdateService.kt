package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.group.GroupRecordArchive
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.GroupRecordArchiveRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GroupRecordArchiveUpdateService(
    private val support: GroupRecordMutationSupport,
    private val archives: GroupRecordArchiveRepository,
    private val notifications: NotificationService,
    private val ids: IntegerIdService,
) {
    @Transactional
    fun updateGroupRecordArchive(
        groupId: Long,
        memberId: Long,
        recordId: Long,
        isInUserArchive: Boolean,
    ) {
        val context = support.getLockedRecordContext(groupId, memberId, recordId, allowStopped = false)
        val (_, card, record) = context
        if (isInUserArchive && (record.isDeleted || card.isDeleted)) {
            throw BadRequestException("삭제된 기록 또는 카드의 기록은 보관할 수 없습니다.")
        }
        val existing = archives.findByMemberIdAndRecordId(memberId, recordId)
        if (isInUserArchive == (existing != null)) return
        if (isInUserArchive) {
            archives.save(GroupRecordArchive(ids.nextId(), memberId, recordId, groupId, Instant.now()))
        } else {
            archives.delete(requireNotNull(existing))
        }
        notifications.createNotification(
            userId = memberId,
            category = NotificationCategory.GROUP,
            title = if (isInUserArchive) "기록 보관" else "기록 보관 해제",
            content = if (isInUserArchive) "기록을 개인 보관함에 보관했습니다." else "기록의 개인 보관을 해제했습니다.",
        )
    }
}
