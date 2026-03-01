package com.sight.repository

import com.sight.domain.fee.FeeHistory
import org.springframework.data.jpa.repository.JpaRepository

interface FeeHistoryRepository : JpaRepository<FeeHistory, Int> {
    fun findByUserIdInAndYearAndSemester(
        userIds: List<Long>,
        year: Int,
        semester: Int,
    ): List<FeeHistory>
}
