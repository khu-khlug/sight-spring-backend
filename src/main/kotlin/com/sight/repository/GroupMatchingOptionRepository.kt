package com.sight.repository

import com.sight.domain.groupmatching.GroupMatchingOption
import com.sight.domain.groupmatching.GroupMatchingType
import org.springframework.data.jpa.repository.JpaRepository

interface GroupMatchingOptionRepository : JpaRepository<GroupMatchingOption, String> {
    fun findAllByGroupMatchingIdAndGroupMatchingType(
        groupMatchingId: String,
        groupMatchingType: GroupMatchingType,
    ): List<GroupMatchingOption>
}
