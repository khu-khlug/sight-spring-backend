package com.sight.core.groupauth

enum class GroupRole {
    /** 그룹원·그룹장이거나 공개 범위에 따라 열람이 허용된 회원. 운영 그룹에는 운영진만 참가한다. */
    VIEWER,

    /** 해당 그룹의 그룹원 또는 그룹장. */
    MEMBER,

    /** 해당 그룹의 현재 그룹장. */
    LEADER,

    /** 해당 그룹의 그룹원·그룹장이면서 해당 그룹에 속한 기록의 작성자. */
    WRITER,

    /** 운영진이면서 대상 그룹이 운영 그룹(MANAGE)인 경우. 그룹 소속 여부는 요구하지 않는다. */
    MANAGER,
}
