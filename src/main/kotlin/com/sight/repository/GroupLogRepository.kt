package com.sight.repository

import com.sight.domain.group.GroupLog
import org.springframework.data.jpa.repository.JpaRepository

interface GroupLogRepository : JpaRepository<GroupLog, Long>
