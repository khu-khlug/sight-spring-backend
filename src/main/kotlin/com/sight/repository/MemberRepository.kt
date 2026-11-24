package com.sight.repository

import com.sight.domain.member.Member
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long>, MemberRepositoryCustom {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.id = :id")
    fun findForUpdateById(id: Long): Member?

    fun findByManagerTrue(): List<Member>

    fun findByNumberIsNotNull(): List<Member>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByNumber(number: Long): Member?

    fun findAllByNumberIn(numbers: Collection<Long>): List<Member>

    fun findByName(name: String): Member?
}
