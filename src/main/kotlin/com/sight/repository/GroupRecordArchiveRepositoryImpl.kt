package com.sight.repository

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.group.QGroup
import com.sight.domain.group.QGroupCard
import com.sight.domain.group.QGroupRecord
import com.sight.domain.group.QGroupRecordArchive
import com.sight.domain.member.QMember
import com.sight.repository.dto.SavedRecordView
import org.springframework.stereotype.Repository

@Repository
class GroupRecordArchiveRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : GroupRecordArchiveRepositoryCustom {
    private val archive = QGroupRecordArchive.groupRecordArchive
    private val record = QGroupRecord.groupRecord
    private val card = QGroupCard.groupCard
    private val group = QGroup.group
    private val author = QMember.member

    override fun findSavedRecordsByMemberIdOrderBySavedAtDesc(
        memberId: Long,
        offset: Long,
        limit: Int,
    ): List<SavedRecordView> =
        queryFactory
            .select(
                Projections.constructor(
                    SavedRecordView::class.java,
                    record.id,
                    record.type,
                    record.content,
                    group.id,
                    group.title,
                    card.id,
                    card.title,
                    record.deletedAt.isNotNull,
                    card.deletedAt.isNotNull,
                    author.realname,
                    record.createdAt,
                    archive.savedAt,
                ),
            )
            .from(archive)
            .join(record).on(archive.recordId.eq(record.id))
            .join(card).on(record.cardId.eq(card.id))
            .join(group).on(archive.groupId.eq(group.id))
            .join(author).on(record.authorMemberId.eq(author.id))
            .where(archive.memberId.eq(memberId))
            .orderBy(archive.savedAt.desc(), record.id.desc())
            .offset(offset)
            .limit(limit.toLong())
            .fetch()
}
