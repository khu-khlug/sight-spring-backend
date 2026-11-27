package com.sight.repository

import java.time.Instant

interface FileUploadMaintenanceRepository {
    fun findCleanupCandidatesAfter(
        cursor: String,
        at: Instant,
        limit: Int,
    ): List<String>
}
