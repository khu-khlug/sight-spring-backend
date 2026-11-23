package com.sight.repository

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.group.QGroupCard
import com.sight.domain.group.QGroupRecord
import com.sight.repository.dto.GroupSearchRecordView
import org.springframework.stereotype.Repository

@Repository
class GroupSearchRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : GroupSearchRepository {
    private val card = QGroupCard.groupCard
    private val record = QGroupRecord.groupRecord

    override fun findSearchableRecordsOrderByLatest(
        groupId: Long,
        offset: Long,
        limit: Int,
    ): List<GroupSearchRecordView> =
        queryFactory
            .select(
                Projections.constructor(
                    GroupSearchRecordView::class.java,
                    record.id,
                    record.cardId,
                    record.type,
                    record.content,
                ),
            ).from(record)
            .join(card).on(record.cardId.eq(card.id))
            .where(card.groupId.eq(groupId), card.deletedAt.isNull, record.deletedAt.isNull)
            .orderBy(record.updatedAt.desc(), record.id.desc())
            .offset(offset)
            .limit(limit.toLong())
            .fetch()
}
