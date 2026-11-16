package com.sight.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.group.GroupRecord
import com.sight.domain.group.GroupRecordSort
import com.sight.domain.group.QGroupRecord
import org.springframework.stereotype.Repository

@Repository
class GroupRecordListRepository(private val queries: JPAQueryFactory) {
    private val record = QGroupRecord.groupRecord

    fun findByCardIdOrderByRecordSort(
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
        return queries.selectFrom(record)
            .where(record.cardId.eq(cardId), if (includeDeleted) null else record.deletedAt.isNull)
            .orderBy(*ordering).offset(offset).limit(limit.toLong()).fetch()
    }

    fun countByCardId(
        cardId: Long,
        includeDeleted: Boolean,
    ): Long =
        queries.select(record.count()).from(record)
            .where(record.cardId.eq(cardId), if (includeDeleted) null else record.deletedAt.isNull).fetchOne() ?: 0
}
