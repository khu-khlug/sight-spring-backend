package com.sight.controllers.http.dto

import java.time.Instant

data class ListDoorLockOccupantsResponse(
    val occupants: List<ListDoorLockOccupantResponse>,
    val lastSyncedAt: Instant?,
)

data class ListDoorLockOccupantResponse(
    val name: String,
)
