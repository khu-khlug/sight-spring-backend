package com.sight.controllers.http.dto

import com.sight.core.exception.BadRequestException

enum class ScheduleListStatus {
    IN_PROGRESS,
    ;

    companion object {
        fun fromQueryParam(value: String): ScheduleListStatus {
            val normalized = value.replace('-', '_').uppercase()
            return entries.firstOrNull { it.name == normalized }
                ?: throw BadRequestException("지원하지 않는 status 값입니다: $value")
        }
    }
}
