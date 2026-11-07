package com.sight.domain.group

enum class GroupVisibility {
    // 그룹원만
    PRIVATE,

    // 교류 회원을 제외한 회원
    MEMBER_ONLY,

    // 모든 로그인 회원
    PUBLIC,

    // 운영진만. 운영 유형 그룹에서만 지정할 수 있다.
    MANAGER_ONLY,
}
