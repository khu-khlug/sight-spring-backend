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
    val khuisAuthAt: LocalDateTime,
    val returnAt: LocalDateTime?,
    val returnReason: String?,
    val lastLoginAt: Instant,
    val lastEnterAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    // TODO: redTags 구현 필요
    //   - "미인증": needAuth() 조건 확인 필요
    //   - "차단": status == UserStatus.INACTIVE
    //   - "-exp": expoint < 0
    // TODO: normalTags 구현 필요 (FeeHistory 엔티티 필요)
    //   - "납부 대상": needPayFee() && 해당 학기 FeeHistory 없음
    //   - "반액 납부 대상": needPayHalfFee() && 해당 학기 FeeHistory 없음
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

fun Member.toResponse(): MemberResponse =
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
    )
