package com.sight.repository

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.group.QGroupCard
import com.sight.domain.group.QGroupKanbanList
import com.sight.domain.group.QGroupRecord
import com.sight.domain.member.QMember
import com.sight.repository.dto.GroupSearchCardView
import com.sight.repository.dto.GroupSearchRecordView
import org.springframework.stereotype.Repository

@Repository
class GroupSearchRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : GroupSearchRepository {
    private val card = QGroupCard.groupCard
    private val list = QGroupKanbanList.groupKanbanList
    private val record = QGroupRecord.groupRecord
    private val member = QMember.member

    override fun findCardsOrderByLatest(
        groupId: Long,
        offset: Long,
        limit: Int,
    ): List<GroupSearchCardView> =
        cardViewQuery()
            .where(card.groupId.eq(groupId), card.deletedAt.isNull)
            .orderBy(card.updatedAt.desc(), card.id.desc())
            .offset(offset)
            .limit(limit.toLong())
            .fetch()

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

    override fun findCardViewsByIds(
        groupId: Long,
        cardIds: Collection<Long>,
    ): List<GroupSearchCardView> {
        if (cardIds.isEmpty()) return emptyList()
        return cardViewQuery()
            .where(card.groupId.eq(groupId), card.deletedAt.isNull, card.id.`in`(cardIds))
            .fetch()
    }

    private fun cardViewQuery() =
        queryFactory
            .select(
                Projections.constructor(
                    GroupSearchCardView::class.java,
                    list.id,
                    list.title,
                    card.id,
                    card.title,
                    member.realname,
                ),
            ).from(card)
            .join(list).on(card.listId.eq(list.id))
            .join(member).on(card.authorMemberId.eq(member.id))
}
