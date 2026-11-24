package com.sight.repository

import com.sight.domain.group.GroupKanbanList
import org.springframework.data.jpa.repository.JpaRepository

interface GroupKanbanListRepository : JpaRepository<GroupKanbanList, Long>
