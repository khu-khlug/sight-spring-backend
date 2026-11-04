package com.sight.repository

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupInterest
import com.sight.domain.group.GroupLog
import com.sight.domain.group.GroupOrderBy
import com.sight.domain.group.GroupStatus
import com.sight.domain.group.QGroup
import com.sight.domain.group.QGroupBookmark
import com.sight.domain.group.QGroupLog
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
    private val groupLog = QGroupLog.groupLog

    override fun findGroups(
        offset: Int,
        limit: Int,
        joined: Boolean?,
        bookmarked: Boolean?,
        categories: List<GroupCategory>?,
        status: GroupStatus?,
        interest: GroupInterest?,
        keyword: String?,
        orderBy: GroupOrderBy?,
        requesterId: Long,
    ): List<GroupListDto> {
        val query =
            queryFactory
                .select(groupListProjection())
                .from(group)
                .join(member).on(group.leaderMemberId.eq(member.id))

        applyJoins(query, joined, bookmarked)
        applyWhere(query, joined, bookmarked, categories, status, interest, keyword, requesterId)

        return query
            .orderBy(resolveOrderBy(orderBy))
            .offset(offset.toLong())
            .limit(limit.toLong())
            .fetch()
    }

    override fun countGroups(
        joined: Boolean?,
        bookmarked: Boolean?,
        categories: List<GroupCategory>?,
        status: GroupStatus?,
        interest: GroupInterest?,
        keyword: String?,
        requesterId: Long,
    ): Long {
        val query =
            queryFactory
                .select(group.countDistinct())
                .from(group)

        applyJoins(query, joined, bookmarked)
        applyWhere(query, joined, bookmarked, categories, status, interest, keyword, requesterId)

        return query.fetchOne() ?: 0L
    }

    override fun findGroupLogsByGroupIdOrderByLatest(
        groupId: Long,
        offset: Int,
        limit: Int,
    ): List<GroupLog> =
        queryFactory
            .selectFrom(groupLog)
            .where(groupLog.groupId.eq(groupId))
            .orderBy(groupLog.createdAt.desc(), groupLog.id.desc())
            .offset(offset.toLong())
            .limit(limit.toLong())
            .fetch()

    override fun countGroupLogsByGroupId(groupId: Long): Long =
        queryFactory
            .select(groupLog.count())
            .from(groupLog)
            .where(groupLog.groupId.eq(groupId))
            .fetchOne() ?: 0L

    private fun <T> applyJoins(
        query: JPAQuery<T>,
        joined: Boolean?,
        bookmarked: Boolean?,
    ) {
        if (joined == true) {
            query.join(groupMember).on(group.id.eq(groupMember.groupId))
        }
        if (bookmarked == true) {
            query.join(groupBookmark).on(group.id.eq(groupBookmark.groupId))
        }
    }

    private fun applyWhere(
        query: JPAQuery<*>,
        joined: Boolean?,
        bookmarked: Boolean?,
        categories: List<GroupCategory>?,
        status: GroupStatus?,
        interest: GroupInterest?,
        keyword: String?,
        requesterId: Long,
    ) {
        val conditions = mutableListOf<BooleanExpression>()

        if (joined == true) {
            conditions.add(groupMember.memberId.eq(requesterId))
        }
        if (bookmarked == true) {
            conditions.add(groupBookmark.memberId.eq(requesterId))
        }

        if (!categories.isNullOrEmpty()) {
            conditions.add(group.category.`in`(categories))
        }

        if (status != null) {
            conditions.add(group.status.eq(status))
        }

        // 관심 분야는 enum 이름을 쉼표로 이어 저장하므로 쉼표 구분자 기준으로 정확히 일치하는 항목을 찾는다.
        if (interest != null) {
            val name = interest.name
            val exactMatch = group.interestNames.eq(name)
            val startsWith = group.interestNames.like("$name,%")
            val contains = group.interestNames.like("%,$name,%")
            val endsWith = group.interestNames.like("%,$name")
            conditions.add(exactMatch.or(startsWith).or(contains).or(endsWith))
        }

        // keyword: 이름, 설명, 사용 기술 중 하나라도 포함되면 매칭 (OR)
        if (!keyword.isNullOrBlank()) {
            val titleLike = group.title.containsIgnoreCase(keyword)
            val descriptionLike = group.description.containsIgnoreCase(keyword)
            val techStackLike = group.techStack.containsIgnoreCase(keyword)
            conditions.add(titleLike.or(descriptionLike).or(techStackLike))
        }

        if (conditions.isNotEmpty()) {
            query.where(*conditions.toTypedArray())
        }
    }

    private fun resolveOrderBy(orderBy: GroupOrderBy?): OrderSpecifier<*> =
        when (orderBy) {
            GroupOrderBy.CHANGED_AT -> group.lastActivityAt.desc()
            else -> group.createdAt.desc()
        }

    private fun groupListProjection() =
        Projections.constructor(
            GroupListDto::class.java,
            group.id,
            group.category,
            group.title,
            group.status,
            group.memberCount,
            group.allowJoin,
            group.createdAt,
            member.id,
            member.realname,
        )
}
