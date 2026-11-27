package com.sight.repository

import com.sight.domain.member.MemberPreference
import org.springframework.data.jpa.repository.JpaRepository

interface MemberPreferenceRepository : JpaRepository<MemberPreference, Long> {
    fun findByMemberId(memberId: Long): MemberPreference?
}
