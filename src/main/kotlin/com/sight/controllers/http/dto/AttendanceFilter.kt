package com.sight.controllers.http.dto

import com.sight.core.exception.BadRequestException

enum class AttendanceFilter {
    ACTIVE,
    ;

    companion object {
        fun fromQueryParam(value: String): AttendanceFilter {
            val normalized = value.replace('-', '_').uppercase()
            return entries.firstOrNull { it.name == normalized }
                ?: throw BadRequestException("지원하지 않는 attendance 값입니다: $value")
        }
    }
}
