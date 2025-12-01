package com.sight.repository

import com.sight.domain.groupmatching.GroupMatching
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface GroupMatchingRepository : JpaRepository<GroupMatching, String> {
    fun existsByYearAndSemester(
        year: Int,
        semester: Int,
    ): Boolean

    fun findAllByClosedAtAfter(now: LocalDateTime): List<GroupMatching>
}
