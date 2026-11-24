package com.sight.domain.file

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.Duration
import java.time.Instant

// 권한 확인을 통과한 요청에서 만료까지 1분 넘게 남은 다운로드 주소를 재사용한다.
@Embeddable
class FileUrlCache(
    @Column(name = "read_url", columnDefinition = "TEXT")
    val url: String,
    @Column(name = "read_url_expires_at", columnDefinition = "TIMESTAMP(6)")
    val expiresAt: Instant,
) {
    init {
        require(url.isNotBlank()) { "읽기 주소가 필요합니다." }
    }

    fun reusableUrl(at: Instant): String? = url.takeIf { expiresAt.isAfter(at.plus(REFRESH_MARGIN)) }

    companion object {
        val REFRESH_MARGIN: Duration = Duration.ofMinutes(1)
    }
}
