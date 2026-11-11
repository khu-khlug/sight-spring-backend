package com.sight.repository

import com.sight.domain.github.GithubCache
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GithubCacheRepository : JpaRepository<GithubCache, String> {
    // 외부 호출 후 최신 캐시를 잠그고 읽어 병합·저장한다. GitHub 호출 동안 잠금을 유지하지 않는다.
    // 아직 없는 행의 동시 삽입과 최대 행 수 제한은 별도의 공통 저장 경로에서 직렬 처리한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cache from GithubCache cache where cache.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: String,
    ): GithubCache?
}
