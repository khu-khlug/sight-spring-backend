package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupRecord
import com.sight.repository.FileUploadRepository
import com.sight.repository.GroupCardRepository
import com.sight.repository.GroupKanbanListRepository
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

@Service
class GroupRecordReadService(
    private val records: GroupRecordRepository,
    private val cards: GroupCardRepository,
    private val lists: GroupKanbanListRepository,
    private val members: MemberRepository,
    private val archives: GroupRecordArchiveRepository,
    private val links: GroupRecordFileRepository,
    private val uploads: FileUploadRepository,
) {
    @Transactional(readOnly = true)
    fun getGroupRecordById(
        groupId: Long,
        recordId: Long,
        memberId: Long,
    ): GroupRecordDetailsResult {
        val record = records.findById(recordId).orElseThrow { NotFoundException("기록을 찾을 수 없습니다.") }
        val card = cards.findById(record.cardId).orElseThrow { NotFoundException("카드를 찾을 수 없습니다.") }
        if (card.groupId != groupId) throw NotFoundException("기록을 찾을 수 없습니다.")
        if (card.isDeleted || record.isDeleted) throw BadRequestException("삭제된 카드 또는 기록입니다.")
        return getRecordDetails(recordId, memberId)
    }

    @Transactional(readOnly = true)
    fun getRecordDetails(
        recordId: Long,
        memberId: Long,
    ): GroupRecordDetailsResult {
        val record = records.findById(recordId).orElseThrow { NotFoundException("기록을 찾을 수 없습니다.") }
        val card = cards.findById(record.cardId).orElseThrow { NotFoundException("카드를 찾을 수 없습니다.") }
        val list = lists.findById(requireNotNull(card.listId)).orElseThrow { NotFoundException("리스트를 찾을 수 없습니다.") }
        return GroupRecordDetailsResult(toRecordResult(record, card.groupId, memberId), card.id, card.title, list.id, list.title)
    }

    @Transactional(readOnly = true)
    fun toRecordResult(
        record: GroupRecord,
        groupId: Long,
        memberId: Long,
    ): GroupRecordResult = listRecordResults(listOf(record), groupId, memberId).single()

    @Transactional(readOnly = true)
    fun listRecordResults(
        records: List<GroupRecord>,
        groupId: Long,
        memberId: Long,
    ): List<GroupRecordResult> {
        if (records.isEmpty()) return emptyList()
        val recordIds = records.map { it.id }
        val fileLinks = links.findByRecordIdInOrderByIdAsc(recordIds)
        val linksByRecord = fileLinks.groupBy { it.recordId }
        val files = uploads.findAllById(fileLinks.map { it.fileUploadId }).associateBy { it.id }
        val authors = members.findAllById(records.map { it.authorMemberId }.distinct()).associateBy { it.id }
        val savedIds = archives.findByMemberIdAndRecordIdIn(memberId, recordIds).map { it.recordId }.toSet()
        return records.map { record ->
            GroupRecordResult(
                record.id, record.type.name,
                authors[record.authorMemberId]?.realname ?: throw NotFoundException("작성자를 찾을 수 없습니다."),
                record.content, record.createdAt, record.updatedAt, record.isDeleted, record.isInPortfolio,
                record.id in savedIds,
                linksByRecord[record.id].orEmpty().mapNotNull { link ->
                    files[link.fileUploadId]?.takeUnless { it.isDeleted }?.let { file ->
                        RecordFileResult(
                            link.fileIdentifier,
                            file.id,
                            if (file.isUploaded) "/groups/$groupId/files/${file.id}" else null,
                            requireNotNull(file.fileName),
                            requireNotNull(file.size),
                            requireNotNull(file.contentType),
                        )
                    }
                },
            )
        }
    }
}
