package com.sight.repository

import com.sight.domain.member.Member
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long>, MemberRepositoryCustom {
    fun findByManagerTrue(): List<Member>

    fun findByNumberIsNotNull(): List<Member>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByNumber(number: Long): Member?
}
