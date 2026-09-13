package com.sight.repository

import com.sight.domain.doorlock.DoorLockOccupant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DoorLockOccupantRepository : JpaRepository<DoorLockOccupant, Long> {
    fun findAllByOrderByUserIdAsc(): List<DoorLockOccupant>
}
