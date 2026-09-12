package com.sight.domain.doorlock

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "door_lock_occupancy_state")
class DoorLockOccupancyState(
    @Id
    @Column(name = "id", nullable = false)
    val id: Int = SINGLETON_ID,
    lastSyncedAt: Instant? = null,
) {
    @Column(name = "last_synced_at")
    var lastSyncedAt: Instant? = lastSyncedAt
        private set

    fun markSyncedAt(syncedAt: Instant) {
        lastSyncedAt = syncedAt
    }

    companion object {
        const val SINGLETON_ID = 1
    }
}
