package com.sight.repository

import com.sight.domain.supportrequest.SupportRequest
import com.sight.domain.supportrequest.SupportRequestCategory
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SupportRequestRepository : JpaRepository<SupportRequest, String> {
    fun findByCategory(
        category: SupportRequestCategory,
        pageable: Pageable,
    ): Page<SupportRequest>

    fun countByCategory(category: SupportRequestCategory): Long

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select supportRequest from SupportRequest supportRequest where supportRequest.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: String,
    ): SupportRequest?
}
