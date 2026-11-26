package com.sight.controllers.http.dto

import com.sight.service.OpenSeminarResult
import java.time.Instant

data class GetSeminarResponse(
    val id: String,
    val isSummerSeason: Boolean,
    val isSpeakAfter: Boolean,
    val scheduledAt: Instant,
    val endAt: Instant,
) {
    companion object {
        fun from(seminar: OpenSeminarResult): GetSeminarResponse =
            GetSeminarResponse(
                id = seminar.id,
                isSummerSeason = seminar.isSummerSeason,
                isSpeakAfter = seminar.isSpeakAfter,
                scheduledAt = seminar.scheduledAt,
                endAt = seminar.endAt,
            )
    }
}
