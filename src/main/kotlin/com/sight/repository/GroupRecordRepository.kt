package com.sight.repository

import com.sight.domain.group.GroupRecord
import com.sight.domain.group.GroupRecordSort
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface GroupRecordRepository : Repository<GroupRecord, Long>, GroupRecordRepositoryCustom {
    fun <S : GroupRecord> save(record: S): S

    fun findByIdAndDeletedAtIsNull(id: Long): GroupRecord?

    @Query("select count(r) > 0 from GroupRecord r where r.id = :id")
    fun existsIncludingDeletedById(
        @Param("id") id: Long,
    ): Boolean

    @Query(
        """
        select count(r) > 0 from GroupRecord r
        join GroupCard c on c.id = r.cardId
        where r.id = :id and c.groupId = :groupId and r.deletedAt is not null
        """,
    )
    fun existsDeletedByIdAndGroupId(
        @Param("id") id: Long,
        @Param("groupId") groupId: Long,
    ): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from GroupRecord r where r.id = :id")
    fun findLockedIncludingDeletedById(
        @Param("id") id: Long,
    ): GroupRecord?
}

interface GroupRecordRepositoryCustom {
    fun findByCardIdOrderByRecordSort(
        cardId: Long,
        includeDeleted: Boolean,
        sort: GroupRecordSort,
        offset: Long,
        limit: Int,
    ): List<GroupRecord>

    fun countByCardId(
        cardId: Long,
        includeDeleted: Boolean,
    ): Long

    fun delete(
        record: GroupRecord,
        at: Instant,
    )
}
