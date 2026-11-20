package com.sight.repository

import com.sight.domain.group.GroupRecordFile
import org.springframework.data.jpa.repository.JpaRepository

interface GroupRecordFileRepository : JpaRepository<GroupRecordFile, Long>, GroupRecordFileRepositoryCustom
