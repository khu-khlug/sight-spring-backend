package com.sight.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.group.GroupMember
import com.sight.domain.group.QGroupCard
import com.sight.domain.group.QGroupMember
import com.sight.domain.group.QGroupRecord
import com.sight.domain.member.QMember
import com.sight.repository.dto.GroupMemberListDto
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class GroupMemberRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : GroupMemberRepositoryCustom {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val groupMember = QGroupMember.groupMember
    private val member = QMember.member
    private val groupCard = QGroupCard.groupCard
    private val groupRecord = QGroupRecord.groupRecord

    override fun findMemberListByGroupId(groupId: Long): List<GroupMemberListDto> {
        val members =
            queryFactory
                .select(member.id, member.realname, member.college)
                .from(groupMember)
                .join(member).on(groupMember.memberId.eq(member.id))
                .where(groupMember.groupId.eq(groupId))
                .orderBy(member.id.asc())
                .fetch()

        if (members.isEmpty()) return emptyList()

        val cardCounts =
            queryFactory
                .select(groupCard.authorMemberId, groupCard.count())
                .from(groupCard)
                .where(groupCard.groupId.eq(groupId), groupCard.deletedAt.isNull)
                .groupBy(groupCard.authorMemberId)
                .fetch()
                .associate { row ->
                    requireNotNull(row.get(groupCard.authorMemberId)) to (row.get(groupCard.count()) ?: 0L)
                }

        val recordCounts =
            queryFactory
                .select(groupRecord.authorMemberId, groupRecord.count())
                .from(groupRecord)
                .join(groupCard).on(groupRecord.cardId.eq(groupCard.id))
                .where(
                    groupCard.groupId.eq(groupId),
                    groupRecord.deletedAt.isNull,
                    groupCard.deletedAt.isNull,
                )
                .groupBy(groupRecord.authorMemberId)
                .fetch()
                .associate { row ->
                    requireNotNull(row.get(groupRecord.authorMemberId)) to (row.get(groupRecord.count()) ?: 0L)
                }

        return members.map { row ->
            val userId = requireNotNull(row.get(member.id))
            GroupMemberListDto(
                userId = userId,
                name = requireNotNull(row.get(member.realname)),
                college = requireNotNull(row.get(member.college)),
                cardCount = cardCounts[userId] ?: 0L,
                recordCount = recordCounts[userId] ?: 0L,
            )
        }
    }

    override fun save(
        id: Long,
        groupId: Long,
        memberId: Long,
        joinedAt: Instant,
    ) {
        entityManager.persist(
            GroupMember(
                id = id,
                groupId = groupId,
                memberId = memberId,
                joinedAt = joinedAt,
            ),
        )
        // 유니크 제약 위반(중복 참가)을 호출 지점의 try-catch에서 즉시 잡을 수 있도록 flush한다.
        entityManager.flush()
    }

    override fun saveAll(groupMembers: List<GroupMember>) {
        groupMembers.forEach { entityManager.persist(it) }
    }

    override fun delete(
        groupId: Long,
        memberId: Long,
    ): Int =
        queryFactory
            .delete(groupMember)
            .where(groupMember.groupId.eq(groupId), groupMember.memberId.eq(memberId))
            .execute()
            .toInt()
}
