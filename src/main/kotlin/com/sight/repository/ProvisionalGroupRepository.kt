package com.sight.repository

import com.sight.domain.groupmatching.ProvisionalGroup
import org.springframework.data.jpa.repository.JpaRepository

interface ProvisionalGroupRepository : JpaRepository<ProvisionalGroup, String> {
    fun findAllByGroupMatchingId(groupMatchingId: String): List<ProvisionalGroup>
}
