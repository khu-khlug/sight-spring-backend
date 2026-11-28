package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotNull

data class UpdateGroupLeaderRequest(
    @field:NotNull(message = "새 그룹장 회원 ID는 필수입니다")
    val userId: Long,
)
