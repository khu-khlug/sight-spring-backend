package com.sight.repository

import com.sight.domain.doorlock.DoorLockOccupancyState
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DoorLockOccupancyStateRepository : JpaRepository<DoorLockOccupancyState, Int> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT state FROM DoorLockOccupancyState state WHERE state.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Int,
    ): DoorLockOccupancyState?
}
