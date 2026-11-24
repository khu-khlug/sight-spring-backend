package com.sight.controllers.http.dto

import com.sight.domain.group.GroupStatus
import jakarta.validation.constraints.NotNull

data class EndGroupRequest(
    // 종료 결과. SUCCESS 또는 FAIL만 허용하며 그 밖의 값은 서비스가 거부한다.
    @field:NotNull(message = "종료 상태는 필수입니다")
    val status: GroupStatus?,
)
