package com.sight.repository

import com.sight.domain.group.GroupCard
import org.springframework.data.jpa.repository.JpaRepository

interface GroupCardRepository : JpaRepository<GroupCard, Long>
