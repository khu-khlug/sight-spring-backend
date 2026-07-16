package com.sight.domain.schedule

enum class ScheduleCategory(val label: String) {
    CLUB("동아리"),
    ACADEMIC("학사"),
    EXTERNAL("외부"),
    MANAGEMENT("운영"),
    GROUP_ACTIVITY("그룹활동"),
    SEMINAR("세미나"),
    AFTERPARTY("뒷풀이"),
    OTHER("기타"),
    ;

    /** 일반 회원이 생성할 수 있는 그룹 활동 카테고리. */
    val isGroupActivity: Boolean get() = this == GROUP_ACTIVITY

    /** 빅세미나(big_seminar) 처리가 동반되는 세미나 카테고리. */
    val isSeminar: Boolean get() = this == SEMINAR

    /** 운영진 전용 카테고리(그룹 활동·세미나를 제외한 전부). */
    val isManagerCategory: Boolean get() = !isGroupActivity && !isSeminar
}
