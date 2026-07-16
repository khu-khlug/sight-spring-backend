package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.sight.core.auth.UserRole
import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.seminar.BigSeminar

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GetScheduleResponse(
    val id: Long,
    val title: String,
    val category: ScheduleCategory,
    val location: String?,
    val state: String,
    val scheduledAt: String,
    val endAt: String,
    val expoint: Int,
    val checkCode: String?,
    val author: Long,
    val createdAt: String,
    val updatedAt: String,
    val isSummerSeason: Boolean?,
    val isSpeakAfter: Boolean?,
) {
    companion object {
        fun from(
            schedule: Schedule,
            role: UserRole,
            bigSeminar: BigSeminar? = null,
        ): GetScheduleResponse {
            return GetScheduleResponse(
                id = schedule.id,
                title = schedule.title,
                category = schedule.category,
                location = schedule.location,
                state = schedule.state.state,
                scheduledAt = schedule.scheduledAt.toString(),
                endAt = schedule.endAt.toString(),
                expoint = schedule.expoint,
                checkCode = if (role == UserRole.MANAGER) schedule.checkCode else null,
                author = schedule.author,
                createdAt = schedule.createdAt.toString(),
                updatedAt = schedule.updatedAt.toString(),
                isSummerSeason = bigSeminar?.isSummerSeason,
                isSpeakAfter = bigSeminar?.isSpeakAfter,
            )
        }
    }
}
