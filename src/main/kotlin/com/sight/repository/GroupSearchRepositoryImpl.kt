package com.sight.repository

import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.StringPath
import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.group.GroupSearchKeywords
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

    override fun findCardsByTitleOrderByLatest(
        groupId: Long,
        keywords: GroupSearchKeywords,
        offset: Long,
        limit: Int,
    ): List<GroupSearchCardView> = findCards(searchableCard(groupId, card.title, keywords), offset, limit)

    override fun countCardsByTitle(
        groupId: Long,
        keywords: GroupSearchKeywords,
    ): Long = countCards(searchableCard(groupId, card.title, keywords))

    override fun findCardsByAuthorNameOrderByLatest(
        groupId: Long,
        keywords: GroupSearchKeywords,
        offset: Long,
        limit: Int,
    ): List<GroupSearchCardView> = findCards(searchableCard(groupId, member.realname, keywords), offset, limit)

    override fun countCardsByAuthorName(
        groupId: Long,
        keywords: GroupSearchKeywords,
    ): Long = countCards(searchableCard(groupId, member.realname, keywords))

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

    private fun findCards(
        condition: Array<BooleanExpression>,
        offset: Long,
        limit: Int,
    ): List<GroupSearchCardView> =
        cardViewQuery()
            .where(*condition)
            .orderBy(card.updatedAt.desc(), card.id.desc())
            .offset(offset)
            .limit(limit.toLong())
            .fetch()

    private fun countCards(condition: Array<BooleanExpression>): Long =
        queryFactory
            .select(card.count())
            .from(card)
            .join(list).on(card.listId.eq(list.id))
            .join(member).on(card.authorMemberId.eq(member.id))
            .where(*condition)
            .fetchOne() ?: 0L

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

    // 그룹의 삭제되지 않은 카드이면서 모든 검색어가 대상 문자열에 들어 있는 조건. 검색어의 `%`, `_`, 이스케이프 문자는 글자 그대로 찾는다.
    private fun searchableCard(
        groupId: Long,
        target: StringPath,
        keywords: GroupSearchKeywords,
    ): Array<BooleanExpression> =
        arrayOf(
            card.groupId.eq(groupId),
            card.deletedAt.isNull,
            *keywords.terms.map { target.containsIgnoreCase(it) }.toTypedArray(),
        )
}
