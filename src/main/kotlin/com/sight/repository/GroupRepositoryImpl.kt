package com.sight.repository

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.group.GroupOrderBy
import com.sight.domain.group.QGroup
import com.sight.domain.group.QGroupBookmark
import com.sight.domain.group.QGroupMember
import com.sight.domain.member.QMember
import com.sight.repository.dto.GroupListDto
import org.springframework.stereotype.Repository

@Repository
class GroupRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : GroupRepositoryCustom {
    private val group = QGroup.group
    private val member = QMember.member
    private val groupBookmark = QGroupBookmark.groupBookmark
    private val groupMember = QGroupMember.groupMember

    override fun findGroups(
        offset: Int,
        limit: Int,
        joined: Boolean?,
        bookmarked: Boolean?,
        orderBy: GroupOrderBy?,
        requesterId: Long,
    ): List<GroupListDto> {
        val query =
            queryFactory
                .select(groupListProjection())
                .from(group)
                .join(member).on(group.master.eq(member.id))

        applyJoins(query, joined, bookmarked)
        applyWhere(query, joined, bookmarked, requesterId)

        return query
            .orderBy(resolveOrderBy(orderBy))
            .offset(offset.toLong())
            .limit(limit.toLong())
            .fetch()
    }

    override fun countGroups(
        joined: Boolean?,
        bookmarked: Boolean?,
        requesterId: Long,
    ): Long {
        val query =
            queryFactory
                .select(group.countDistinct())
                .from(group)

        applyJoins(query, joined, bookmarked)
        applyWhere(query, joined, bookmarked, requesterId)

        return query.fetchOne() ?: 0L
    }

    private fun <T> applyJoins(
        query: JPAQuery<T>,
        joined: Boolean?,
        bookmarked: Boolean?,
    ) {
        if (joined == true) {
            query.join(groupMember).on(group.id.eq(groupMember.group))
        }
        if (bookmarked == true) {
            query.join(groupBookmark).on(group.id.eq(groupBookmark.group))
        }
    }

    private fun applyWhere(
        query: JPAQuery<*>,
        joined: Boolean?,
        bookmarked: Boolean?,
        requesterId: Long,
    ) {
        val conditions = mutableListOf<BooleanExpression>()

        if (joined == true) {
            conditions.add(groupMember.member.eq(requesterId))
        }
        if (bookmarked == true) {
            conditions.add(groupBookmark.member.eq(requesterId))
        }

        if (conditions.isNotEmpty()) {
            query.where(*conditions.toTypedArray())
        }
    }

    private fun resolveOrderBy(orderBy: GroupOrderBy?): OrderSpecifier<*> =
        when (orderBy) {
            GroupOrderBy.CHANGED_AT -> group.changedAt.desc()
            else -> group.createdAt.desc()
        }

    private fun groupListProjection() =
        Projections.constructor(
            GroupListDto::class.java,
            group.id,
            group.category.stringValue(),
            group.title,
            group.state.stringValue(),
            group.countMember,
            group.allowJoin,
            group.createdAt,
            member.id,
            member.realname,
        )
}
