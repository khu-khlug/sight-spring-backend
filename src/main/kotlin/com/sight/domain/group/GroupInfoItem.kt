package com.sight.domain.group

// 그룹 정보 변경에서 실제로 바뀐 항목. 변경 알림에 바뀐 항목을 담는 데 쓴다.
enum class GroupInfoItem {
    TITLE,
    DESCRIPTION,
    CATEGORY,
    INTERESTS,
    TECH_STACK,
    REPOSITORY_URLS,
    ALLOW_JOIN,
    VISIBILITY,
}
