package com.sight.service

interface PointClient {
    fun givePoint(
        targetUserId: Long,
        point: Int,
        message: String,
    )
}
