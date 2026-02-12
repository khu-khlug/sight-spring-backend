package com.sight.domain.schedule

enum class ScheduleCategory(val code: Long, val label: String) {
    ROOM_405(32529, "405호"),
    ROOM_406(32530, "406호"),
    ROOM_410(32531, "410호"),
    CLUB(7742, "동아리"),
    ACADEMIC(7743, "학사"),
    EXTERNAL(7744, "외부"),
    ;

    companion object {
        fun fromCode(code: Long): ScheduleCategory? = entries.find { it.code == code }
    }
}
