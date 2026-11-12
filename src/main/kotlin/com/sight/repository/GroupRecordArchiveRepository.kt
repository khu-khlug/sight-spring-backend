package com.sight.repository

import com.sight.domain.group.GroupRecordArchive
import org.springframework.data.jpa.repository.JpaRepository

interface GroupRecordArchiveRepository : JpaRepository<GroupRecordArchive, Long>, GroupRecordArchiveRepositoryCustom {
    fun countByMemberId(memberId: Long): Long
}
