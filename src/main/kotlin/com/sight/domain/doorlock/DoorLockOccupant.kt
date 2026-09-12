package com.sight.domain.doorlock

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "door_lock_occupant")
data class DoorLockOccupant(
    @Id
    @Column(name = "user_id", nullable = false)
    val userId: Long,
)
