package com.sight.repository

import com.sight.repository.dto.FileUploadReservationView
import java.time.Instant

interface FileUploadMaintenanceRepository {
    fun findPendingReservationsAfter(
        cursor: String,
        horizon: Instant,
        limit: Int,
    ): List<FileUploadReservationView>

    fun findCleanupCandidatesAfter(
        cursor: String,
        at: Instant,
        limit: Int,
    ): List<String>
}
