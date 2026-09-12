package com.sight.repository

import com.sight.domain.supportrequest.SupportRequest
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SupportRequestRepository : JpaRepository<SupportRequest, String>, SupportRequestRepositoryCustom {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select supportRequest from SupportRequest supportRequest where supportRequest.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: String,
    ): SupportRequest?
}
