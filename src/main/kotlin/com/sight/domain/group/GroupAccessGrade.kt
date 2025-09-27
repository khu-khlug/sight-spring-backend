package com.sight.domain.group

// 참고: `code = 1`에 대한 level은 회장 공개였으나, 회장 개념이 쿠러그에서 사라져 사용되지 않고 있습니다.
enum class GroupAccessGrade(val code: Int) {
    PRIVATE(0),
    MANAGER(2),
    MEMBER(3),
    ALL(4),
}
