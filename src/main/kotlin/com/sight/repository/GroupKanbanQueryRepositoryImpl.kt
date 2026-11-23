package com.sight.repository

import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.QFileReference
import com.sight.domain.group.QGroupCard
import com.sight.domain.group.QGroupKanbanList
import com.sight.domain.group.QGroupRecord
import com.sight.domain.member.QMember
import org.springframework.stereotype.Repository

@Repository
class GroupKanbanQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : GroupKanbanQueryRepository {
    private val list = QGroupKanbanList.groupKanbanList
    private val card = QGroupCard.groupCard

    override fun countListsByGroupId(groupId: Long): Long =
        queryFactory.select(list.count()).from(list).where(list.groupId.eq(groupId)).fetchOne() ?: 0L

    override fun findListsByGroupIdOrderByPosition(
        groupId: Long,
        offset: Long,
        limit: Int,
    ): List<GroupKanbanListView> =
        queryFactory
            .select(list.id, list.title, list.description)
            .from(list)
            .where(list.groupId.eq(groupId))
            .orderBy(list.position.asc(), list.id.asc())
            .offset(offset)
            .limit(limit.toLong())
            .fetch()
            .map { GroupKanbanListView(checkNotNull(it[list.id]), checkNotNull(it[list.title]), it[list.description].orEmpty()) }

    override fun findCardSummariesByListIdsOrderByPosition(
        groupId: Long,
        listIds: List<Long>,
        offset: Long,
        limit: Int,
    ): List<GroupCardSummaryView> {
        if (listIds.isEmpty() || limit <= 0) return emptyList()

        val visibleCards = card.groupId.eq(groupId).and(card.listId.`in`(listIds)).and(card.deletedAt.isNull)
        val cardCount = card.count()
        val countsByListId =
            queryFactory.select(card.listId, cardCount)
                .from(card)
                .where(visibleCards)
                .groupBy(card.listId)
                .fetch()
                .associate { it[card.listId] to checkNotNull(it[cardCount]) }

        val precedingCard = QGroupCard("precedingCard")
        // 같은 리스트에서 앞선 카드의 수는 0부터 시작하는 카드 순번이다.
        val rank =
            JPAExpressions.select(precedingCard.count())
                .from(precedingCard)
                .where(
                    precedingCard.groupId.eq(card.groupId),
                    precedingCard.listId.eq(card.listId),
                    precedingCard.deletedAt.isNull,
                    precedingCard.position.lt(card.position)
                        .or(precedingCard.position.eq(card.position).and(precedingCard.id.lt(card.id))),
                )
        val author = QMember("author")
        val assignee = QMember("assignee")
        val record = QGroupRecord.groupRecord
        val recordCount =
            JPAExpressions.select(record.count())
                .from(record)
                .where(record.cardId.eq(card.id), record.deletedAt.isNull)
        val hasDescription = card.description.length().gt(0)
        val lastRank = offset + minOf(limit.toLong() - 1, Long.MAX_VALUE - offset)
        val inPage = Expressions.booleanTemplate("{0} between {1} and {2}", rank, offset, lastRank)
        val rows =
            queryFactory.select(
                card.listId, card.id, card.title, author.realname, recordCount, hasDescription,
                card.isInPortfolio, card.assigneeMemberId, assignee.realname, card.labelBits, card.isDisabled, card.createdAt,
            )
                .from(card)
                .leftJoin(author).on(author.id.eq(card.authorMemberId))
                .leftJoin(assignee).on(assignee.id.eq(card.assigneeMemberId))
                .where(visibleCards, inPage)
                .orderBy(card.listId.asc(), card.position.asc(), card.id.asc())
                .fetch()
        if (rows.isEmpty()) return emptyList()

        val cover = QFileReference.fileReference
        val coversByCardId =
            queryFactory.select(cover.ownerId, cover.fileUploadId)
                .from(cover)
                .where(
                    cover.referenceType.eq(FileReferenceType.GROUP_CARD_COVER),
                    cover.ownerId.`in`(rows.map { checkNotNull(it[card.id]).toString() }),
                )
                .orderBy(cover.id.asc())
                .fetch()
                .distinctBy { it[cover.ownerId] }
                .associate { it[cover.ownerId] to it[cover.fileUploadId] }

        return rows.map { row ->
            val listId = checkNotNull(row[card.listId])
            val cardId = checkNotNull(row[card.id])
            GroupCardSummaryView(
                listId = listId,
                cardCount = countsByListId.getValue(listId),
                id = cardId,
                title = checkNotNull(row[card.title]),
                authorName = row[author.realname].orEmpty(),
                recordCount = checkNotNull(row[recordCount]),
                hasDescription = row[hasDescription] == true,
                isInPortfolio = row[card.isInPortfolio] == true,
                assigneeUserId = row[card.assigneeMemberId],
                assigneeName = row[assignee.realname],
                coverFileId = coversByCardId[cardId.toString()],
                labelBits = checkNotNull(row[card.labelBits]),
                isDisabled = row[card.isDisabled] == true,
                createdAt = checkNotNull(row[card.createdAt]),
            )
        }
    }
}
