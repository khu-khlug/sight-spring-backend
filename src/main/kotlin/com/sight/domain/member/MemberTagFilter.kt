package com.sight.domain.member

enum class MemberTagFilter {
    UNAUTHORIZED, // 미인증
    BLOCKED, // 차단
    MINUS_EXP, // -exp
    FEE_TARGET, // 납부 대상
    HALF_FEE_TARGET, // 반액 납부 대상
}
