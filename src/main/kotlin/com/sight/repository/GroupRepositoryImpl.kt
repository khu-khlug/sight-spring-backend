package com.sight.repository

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupOrderBy
import com.sight.domain.group.GroupState
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
        categories: List<GroupCategory>?,
        state: GroupState?,
        interest: String?,
        keyword: String?,
        orderBy: GroupOrderBy?,
        requesterId: Long,
    ): List<GroupListDto> {
        val query =
            queryFactory
                .select(groupListProjection())
                .from(group)
                .join(member).on(group.master.eq(member.id))

        applyJoins(query, joined, bookmarked)
        applyWhere(query, joined, bookmarked, categories, state, interest, keyword, requesterId)

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
        state: GroupState?,
        interest: String?,
        keyword: String?,
        requesterId: Long,
    ): Long {
        val query =
            queryFactory
                .select(group.countDistinct())
                .from(group)

        applyJoins(query, joined, bookmarked)
        applyWhere(query, joined, bookmarked, categories, state, interest, keyword, requesterId)

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
        categories: List<GroupCategory>?,
        state: GroupState?,
        interest: String?,
        keyword: String?,
        requesterId: Long,
    ) {
        val conditions = mutableListOf<BooleanExpression>()

        if (joined == true) {
            conditions.add(groupMember.member.eq(requesterId))
        }
        if (bookmarked == true) {
            conditions.add(groupBookmark.member.eq(requesterId))
        }

        // category IN 조건
        if (!categories.isNullOrEmpty()) {
            conditions.add(group.category.`in`(categories))
        }

        // state 일치 조건
        if (state != null) {
            conditions.add(group.state.eq(state))
        }

        // interest: pipe(|) 구분자 기준 정확 매칭
        // DB 값 예시: "웹|AI|보안" → interest=웹 으로 검색 시 매칭되어야 함
        if (!interest.isNullOrBlank()) {
            val exactMatch = group.interest.eq(interest)
            val startsWith = group.interest.like("$interest|%")
            val contains = group.interest.like("%|$interest|%")
            val endsWith = group.interest.like("%|$interest")
            conditions.add(exactMatch.or(startsWith).or(contains).or(endsWith))
        }

        // keyword: title, purpose, technology 중 하나라도 포함되면 매칭 (OR)
        if (!keyword.isNullOrBlank()) {
            val titleLike = group.title.containsIgnoreCase(keyword)
            val purposeLike = group.purpose.containsIgnoreCase(keyword)
            val technologyLike = group.technology.containsIgnoreCase(keyword)
            conditions.add(titleLike.or(purposeLike).or(technologyLike))
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
