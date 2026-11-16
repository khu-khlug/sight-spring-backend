package com.sight.repository

import com.sight.domain.group.GroupRecord
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface GroupRecordRepository : JpaRepository<GroupRecord, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from GroupRecord r where r.id = :id")
    fun findLockedById(id: Long): GroupRecord?
}
