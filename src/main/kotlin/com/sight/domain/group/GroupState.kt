package com.sight.domain.group

enum class GroupState(val value: String) {
    PENDING("pending"), // 보류
    PROGRESS("progress"), // 진행
    SUSPEND("suspend"), // 중단
    END_SUCCESS("end-success"), // 종료 (성공)
    END_FAIL("end-fail"), // 종료 (실패)
}
