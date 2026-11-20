package com.sight.domain.group

enum class GroupCategory {
    PROJECT,
    STUDY,
    DOCUMENTATION,
    MANAGE,
    EDUCATION,
    PROGRAM,
    ;

    // 운영진만 지정할 수 있는 유형인지 여부. 운영진이 아니면 프로젝트·스터디·문서화만 지정할 수 있다.
    val isManagerOnly: Boolean
        get() = this == MANAGE || this == EDUCATION || this == PROGRAM
}
