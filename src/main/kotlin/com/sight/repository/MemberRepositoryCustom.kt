package com.sight.repository

import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus

interface MemberRepositoryCustom {
    fun findMembers(
        email: String?,
        phone: String?,
        name: String?,
        number: String?,
        college: String?,
        grade: Int?,
        studentStatus: StudentStatus?,
        limit: Int,
        offset: Int,
    ): List<Member>

    fun countMembers(
        email: String?,
        phone: String?,
        name: String?,
        number: String?,
        college: String?,
        grade: Int?,
        studentStatus: StudentStatus?,
    ): Long
}
