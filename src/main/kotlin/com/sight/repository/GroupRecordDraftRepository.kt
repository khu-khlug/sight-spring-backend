package com.sight.repository

import com.sight.domain.group.GroupRecordDraft
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface GroupRecordDraftRepository : JpaRepository<GroupRecordDraft, Long> {
    fun findByMemberId(memberId: Long): GroupRecordDraft?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from GroupRecordDraft d where d.memberId = :memberId")
    fun findForUpdateByMemberId(memberId: Long): GroupRecordDraft?
}
