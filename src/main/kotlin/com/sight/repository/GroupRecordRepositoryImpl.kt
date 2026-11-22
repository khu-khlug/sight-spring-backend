package com.sight.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.group.GroupRecord
import com.sight.domain.group.GroupRecordSort
import com.sight.domain.group.QGroupRecord
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class GroupRecordRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : GroupRecordRepositoryCustom {
    private val record = QGroupRecord.groupRecord

    override fun findByCardIdOrderByRecordSort(
        cardId: Long,
        includeDeleted: Boolean,
        sort: GroupRecordSort,
        offset: Long,
        limit: Int,
    ): List<GroupRecord> {
        val ordering =
            when (sort) {
                GroupRecordSort.NEWEST -> arrayOf(record.createdAt.desc(), record.id.desc())
                GroupRecordSort.OLDEST -> arrayOf(record.createdAt.asc(), record.id.asc())
                GroupRecordSort.RECENTLY_UPDATED -> arrayOf(record.updatedAt.desc(), record.id.desc())
            }
        return queryFactory
            .selectFrom(record)
            .where(record.cardId.eq(cardId), if (includeDeleted) null else record.deletedAt.isNull)
            .orderBy(*ordering)
            .offset(offset)
            .limit(limit.toLong())
            .fetch()
    }

    override fun countByCardId(
        cardId: Long,
        includeDeleted: Boolean,
    ): Long =
        queryFactory
            .select(record.count())
            .from(record)
            .where(record.cardId.eq(cardId), if (includeDeleted) null else record.deletedAt.isNull)
            .fetchOne() ?: 0

    override fun delete(
        record: GroupRecord,
        at: Instant,
    ) {
        record.delete(at)
    }
}
