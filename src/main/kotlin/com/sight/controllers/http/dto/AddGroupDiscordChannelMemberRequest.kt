package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

data class AddGroupDiscordChannelMemberRequest(
    @field:NotNull(message = "멤버 ID는 필수입니다")
    @field:JsonProperty("memberId")
    val memberId: Long?,
)
