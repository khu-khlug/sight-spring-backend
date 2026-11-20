package com.sight.domain.group

// 그룹 정보 변경에서 실제로 바뀐 항목. 변경 알림과 활동 로그에 바뀐 항목을 담는 데 쓴다.
enum class GroupInfoItem(
    // 알림과 활동 로그에 표시하는 항목 이름.
    val displayName: String,
) {
    TITLE("이름"),
    DESCRIPTION("설명"),
    CATEGORY("유형"),
    INTERESTS("관심 분야"),
    TECH_STACK("사용 기술"),
    REPOSITORY_URLS("저장소"),
    ALLOW_JOIN("참여 신청 가능 여부"),
    VISIBILITY("공개 범위"),
}
