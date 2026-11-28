package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.group.GroupRecordArchive
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.GroupRecordArchiveRepository
import com.sight.repository.dto.SavedRecordView
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class SavedRecordListResult(
    val page: Int,
    val totalCount: Long,
    val receivedCount: Int,
    val records: List<SavedRecordView>,
)

@Service
class GroupRecordArchiveService(
    private val groupRecordArchiveRepository: GroupRecordArchiveRepository,
    private val support: GroupRecordMutationSupport,
    private val notifications: NotificationService,
    private val ids: IntegerIdService,
) {
    // 요청자가 모든 그룹에서 보관한 기록을 조회한다. 그룹 상태·소속 여부·원본 삭제 여부로 거르지 않는다.
    @Transactional(readOnly = true)
    fun listSavedRecords(
        memberId: Long,
        page: Int,
    ): SavedRecordListResult {
        val offset = page.toLong() * PAGE_SIZE
        val records =
            groupRecordArchiveRepository.findSavedRecordsByMemberIdOrderBySavedAtDesc(
                memberId = memberId,
                offset = offset,
                limit = PAGE_SIZE,
            )
        val totalCount = groupRecordArchiveRepository.countByMemberId(memberId)

        return SavedRecordListResult(
            page = page,
            totalCount = totalCount,
            receivedCount = records.size,
            records = records,
        )
    }

    @Transactional
    fun updateGroupRecordArchive(
        groupId: Long,
        memberId: Long,
        recordId: Long,
        requestedArchiveState: Boolean,
    ) {
        val context = support.getLockedRecordContext(groupId, memberId, recordId, allowStopped = false)
        val (_, card, record) = context
        if (requestedArchiveState && (record.isDeleted || card.isDeleted)) {
            throw BadRequestException("삭제된 기록 또는 카드의 기록은 보관할 수 없습니다.")
        }
        val existingArchive = groupRecordArchiveRepository.findByMemberIdAndRecordId(memberId, recordId)
        if (requestedArchiveState == (existingArchive != null)) return
        if (requestedArchiveState) {
            groupRecordArchiveRepository.save(GroupRecordArchive(ids.nextId(), memberId, recordId, groupId, Instant.now()))
        } else {
            groupRecordArchiveRepository.delete(requireNotNull(existingArchive))
        }
        notifications.createNotification(
            userId = memberId,
            category = NotificationCategory.GROUP,
            title = if (requestedArchiveState) "기록 보관" else "기록 보관 해제",
            content = if (requestedArchiveState) "기록을 개인 보관함에 보관했습니다." else "기록의 개인 보관을 해제했습니다.",
        )
    }

    companion object {
        // 한 번에 반환하는 최대 기록 수. 본문 전체를 담아 항목이 무거우므로 카드창 기록 목록과 같은 크기다.
        const val PAGE_SIZE: Int = 10
    }
}
