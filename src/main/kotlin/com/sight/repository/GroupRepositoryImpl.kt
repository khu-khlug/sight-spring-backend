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
import com.sight.repository.dto.GroupLogListDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class GroupRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
    private val jdbcTemplate: JdbcTemplate,
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

    override fun findGroupLogsByGroupId(
        groupId: Long,
        offset: Int,
        limit: Int,
    ): List<GroupLogListDto> {
        return jdbcTemplate.query(
            """
            SELECT id, member, message, created_at
            FROM khlug_group_log
            WHERE `group` = ?
            ORDER BY created_at DESC, id DESC
            LIMIT ? OFFSET ?
            """.trimIndent(),
            { rs, _ ->
                GroupLogListDto(
                    id = rs.getLong("id"),
                    memberId = rs.getLong("member"),
                    message = rs.getString("message"),
                    createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
                )
            },
            groupId,
            limit,
            offset,
        )
    }

    override fun countGroupLogsByGroupId(groupId: Long): Long {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM khlug_group_log WHERE `group` = ?",
            Long::class.java,
            groupId,
        )
    }

    override fun insertGroupLog(
        id: Long,
        groupId: Long,
        memberId: Long,
        message: String,
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO khlug_group_log (id, `group`, member, message)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
            id,
            groupId,
            memberId,
            message,
        )
    }

    override fun incrementCountMember(groupId: Long) {
        jdbcTemplate.update(
            "UPDATE khlug_group SET count_member = count_member + 1 WHERE id = ?",
            groupId,
        )
    }

    override fun decrementCountMember(groupId: Long) {
        jdbcTemplate.update(
            "UPDATE khlug_group SET count_member = count_member - 1 WHERE id = ?",
            groupId,
        )
    }

    override fun touchChangedAtAndPromoteFromSuspend(groupId: Long) {
        jdbcTemplate.update(
            "UPDATE khlug_group SET changed_at = now() WHERE id = ?",
            groupId,
        )
        jdbcTemplate.update(
            "UPDATE khlug_group SET state = 'progress' WHERE id = ? AND state = 'suspend'",
            groupId,
        )
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
