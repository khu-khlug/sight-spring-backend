package com.sight.controllers.http.dto

import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import java.time.Instant
import java.time.LocalDateTime

data class ListMemberResponse(
    val count: Long,
    val users: List<MemberResponse>,
)

data class MemberResponse(
    val id: Long,
    val name: String,
    val profile: MemberProfileResponse,
    val admission: String,
    val studentStatus: StudentStatus,
    val point: Long,
    val status: UserStatus,
    val manager: Boolean,
    val slack: String?,
    val rememberToken: String?,
    val khuisAuthAt: Instant,
    val returnAt: LocalDateTime?,
    val returnReason: String?,
    val lastLoginAt: Instant,
    val lastEnterAt: LocalDateTime,
    val createdAt: Instant,
    val updatedAt: LocalDateTime,
    val normalTags: List<String>,
    val redTags: List<String>,
)

data class MemberProfileResponse(
    val name: String,
    val college: String,
    val grade: Long,
    val number: Long?,
    val email: String?,
    val phone: String?,
    val homepage: String?,
    val language: String?,
    val prefer: String?,
)

fun Member.toResponse(
    normalTags: List<String>,
    redTags: List<String>,
): MemberResponse =
    MemberResponse(
        id = id,
        name = name,
        profile =
            MemberProfileResponse(
                name = realname,
                college = college,
                grade = grade,
                number = number,
                email = email,
                phone = phone,
                homepage = homepage,
                language = language,
                prefer = prefer,
            ),
        admission = admission,
        studentStatus = studentStatus,
        point = expoint,
        status = status,
        manager = manager,
        slack = slack,
        rememberToken = rememberToken,
        khuisAuthAt = khuisauthAt,
        returnAt = returnAt,
        returnReason = returnReason,
        lastLoginAt = lastLogin,
        lastEnterAt = lastEnter,
        createdAt = createdAt,
        updatedAt = updatedAt,
        normalTags = normalTags,
        redTags = redTags,
    )
