package com.sight.controllers.http.dto

import com.sight.domain.member.StudentStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class ListMemberRequest(
    val email: String? = null,
    val phone: String? = null,
    val name: String? = null,
    val number: String? = null,
    val college: String? = null,
    val grade: Int? = null,
    val studentStatus: StudentStatus? = null,

    @field:Min(1)
    @field:Max(50)
    val limit: Int,

    @field:Min(0)
    val offset: Int,
)
