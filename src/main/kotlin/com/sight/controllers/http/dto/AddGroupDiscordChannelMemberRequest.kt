package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotNull

data class AddGroupDiscordChannelMemberRequest(
    @field:NotNull(message = "멤버 ID는 필수입니다")
    val memberId: Long,
)
