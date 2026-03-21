package com.sight.controllers.http.dto

import com.sight.domain.groupmatching.GroupMatchingType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class CreateGroupMatchingRequest(
    @field:NotNull
    val year: Int,
    @field:NotNull
    val semester: Int,
    @field:NotNull
    val closedAt: LocalDate,
    @field:Valid
    val options: List<OptionItem> = emptyList(),
) {
    data class OptionItem(
        @field:NotBlank(message = "옵션 이름은 필수입니다")
        val name: String,
        @field:NotNull(message = "옵션 타입은 필수입니다")
        val type: GroupMatchingType,
    )
}
