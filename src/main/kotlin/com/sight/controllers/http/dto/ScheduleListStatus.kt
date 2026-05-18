package com.sight.controllers.http.dto

import com.sight.core.exception.BadRequestException

enum class ScheduleListStatus {
    ACTIVE,
    ;

    companion object {
        fun fromQueryParam(value: String): ScheduleListStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw BadRequestException("지원하지 않는 status 값입니다: $value")
        }
    }
}
