package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.sight.core.auth.UserRole
import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.seminar.BigSeminar

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GetScheduleResponse(
    val id: String,
    val title: String,
    val category: ScheduleCategory,
    val location: String?,
    val state: String,
    val scheduledAt: String,
    val endAt: String,
    val expoint: Int,
    val checkCode: String?,
    val author: Long,
    val authorName: String?,
    val groupId: String?,
    val groupTitle: String?,
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
            authorName: String? = null,
            groupTitle: String? = null,
        ): GetScheduleResponse {
            return GetScheduleResponse(
                id = schedule.id.toString(),
                title = schedule.title,
                category = schedule.category,
                location = schedule.location,
                state = schedule.state.state,
                scheduledAt = schedule.scheduledAt.toString(),
                endAt = schedule.endAt.toString(),
                expoint = schedule.expoint,
                checkCode = if (role == UserRole.MANAGER) schedule.checkCode else null,
                author = schedule.author,
                authorName = authorName,
                groupId = schedule.groupId?.toString(),
                groupTitle = groupTitle,
                createdAt = schedule.createdAt.toString(),
                updatedAt = schedule.updatedAt.toString(),
                isSummerSeason = bigSeminar?.isSummerSeason,
                isSpeakAfter = bigSeminar?.isSpeakAfter,
            )
        }
    }
}
