package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.file.FileReferenceType
import com.sight.domain.group.GroupRecord
import com.sight.repository.GroupCardRepository
import com.sight.repository.GroupKanbanListRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRecordArchiveRepository
import com.sight.repository.GroupRecordFileRepository
import com.sight.repository.GroupRecordRepository
import com.sight.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class GroupRecordResult(
    val id: Long,
    val type: String,
    val authorName: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isDeleted: Boolean,
    val isInPortfolio: Boolean,
    val isInUserArchive: Boolean,
    val files: List<RecordFileResult>,
)

data class GroupRecordDetailsResult(
    val record: GroupRecordResult,
    val cardId: Long,
    val cardTitle: String,
    val listId: Long,
    val listTitle: String,
)

data class GroupRecordListResult(val offset: Long, val totalCount: Long, val records: List<GroupRecordResult>)

@Service
class GroupRecordReadService(
    private val records: GroupRecordRepository,
    private val cards: GroupCardRepository,
    private val lists: GroupKanbanListRepository,
    private val members: MemberRepository,
    private val archives: GroupRecordArchiveRepository,
    private val recordFileRepository: GroupRecordFileRepository,
    private val groupMembers: GroupMemberRepository,
) {
    @Transactional(readOnly = true)
    fun getGroupRecordById(
        groupId: Long,
        recordId: Long,
        memberId: Long,
    ): GroupRecordDetailsResult {
        val record =
            records.findByIdAndDeletedAtIsNull(recordId) ?: run {
                if (records.existsDeletedByIdAndGroupId(recordId, groupId)) throw BadRequestException("삭제된 카드 또는 기록입니다.")
                throw NotFoundException("기록을 찾을 수 없습니다.")
            }
        val card = cards.findById(record.cardId).orElseThrow { NotFoundException("카드를 찾을 수 없습니다.") }
        if (card.groupId != groupId) throw NotFoundException("기록을 찾을 수 없습니다.")
        if (card.isDeleted) throw BadRequestException("삭제된 카드 또는 기록입니다.")
        val list = lists.findById(requireNotNull(card.listId)).orElseThrow { NotFoundException("리스트를 찾을 수 없습니다.") }
        return GroupRecordDetailsResult(toRecordResult(record, card.groupId, memberId), card.id, card.title, list.id, list.title)
    }

    private fun toRecordResult(
        record: GroupRecord,
        groupId: Long,
        memberId: Long,
    ): GroupRecordResult = listRecordResults(listOf(record), groupId, memberId).single()

    private fun listRecordResults(
        records: List<GroupRecord>,
        groupId: Long,
        memberId: Long,
    ): List<GroupRecordResult> {
        if (records.isEmpty()) return emptyList()
        val recordIds = records.map { it.id }
        val attachments =
            recordFileRepository.findByRefTypeAndReferrerInOrderById(FileReferenceType.GROUP_RECORD_ATTACHMENT, recordIds.map { it.toString() })
        val attachmentsByRecordId = attachments.groupBy { it.fileReference.referrerId }
        val authors = members.findAllById(records.map { it.authorMemberId }.distinct()).associateBy { it.id }
        val savedIds = archives.findByMemberIdAndRecordIdIn(memberId, recordIds).map { it.recordId }.toSet()
        return records.map { record ->
            GroupRecordResult(
                record.id, record.type.name,
                authors[record.authorMemberId]?.realname ?: throw NotFoundException("작성자를 찾을 수 없습니다."),
                record.content, record.createdAt, record.updatedAt, record.isDeleted, record.isInPortfolio,
                record.id in savedIds,
                attachmentsByRecordId[record.id.toString()].orEmpty().mapNotNull { attachment ->
                    attachment.fileUpload.takeUnless { it.isDeleted }?.let { fileUpload ->
                        RecordFileResult(
                            attachment.recordFile.fileBlockId,
                            fileUpload.id,
                            if (fileUpload.isUploaded) GroupFilePaths.fileUrl(groupId, fileUpload.id) else null,
                            requireNotNull(fileUpload.fileName),
                            requireNotNull(fileUpload.size),
                            requireNotNull(fileUpload.contentType),
                        )
                    }
                },
            )
        }
    }

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
        if (includeDeleted && !groupMembers.existsByGroupIdAndMemberId(groupId, memberId)) {
            throw ForbiddenException("그룹원만 삭제된 기록을 조회할 수 있습니다.")
        }
        val totalCount = records.countByCardId(cardId, includeDeleted)
        if (offset >= totalCount) return GroupRecordListResult(offset, totalCount, emptyList())
        val page = records.findByCardIdOrderByRecordSort(cardId, includeDeleted, card.recordSort, offset, limit)
        return GroupRecordListResult(offset, totalCount, listRecordResults(page, groupId, memberId))
    }
}
