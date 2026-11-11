package com.sight.domain.group

enum class GroupStatus {
    PROGRESS,
    STOP,
    SUCCESS,
    FAIL,
    ;

    // 성공·실패 종료 상태인지 여부. 종료된 그룹은 활동만으로 진행 상태로 돌아가지 않는다.
    val isEnded: Boolean
        get() = this == SUCCESS || this == FAIL
}
