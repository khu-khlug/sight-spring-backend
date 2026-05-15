package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotNull

data class UpdateGroupMasterRequest(
    @field:NotNull(message = "위임 대상 회원 ID는 필수입니다")
    val memberId: Long,
)
