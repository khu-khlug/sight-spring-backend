package com.sight.repository

import com.sight.domain.groupmatching.GroupMatchingField
import org.springframework.data.jpa.repository.JpaRepository

interface GroupMatchingFieldRepository : JpaRepository<GroupMatchingField, String> {
    fun existsByName(name: String): Boolean
}
