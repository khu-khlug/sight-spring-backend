package com.sight.repository

import com.sight.domain.github.GithubApiUsage
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GithubApiUsageRepository : JpaRepository<GithubApiUsage, Long> {
    // 회원의 사용량 행을 잠그고 조회한다. 같은 회원의 동시 요청이 남은 할당량을 함께 예약해 시간당 한도를 넘지 않게 한다.
    // 잠금은 예약·확정 트랜잭션 동안만 유지하고, GitHub 호출은 예약을 커밋한 뒤 잠금 밖에서 한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select usage from GithubApiUsage usage where usage.memberId = :memberId")
    fun findByMemberIdForUpdate(
        @Param("memberId") memberId: Long,
    ): GithubApiUsage?
}
