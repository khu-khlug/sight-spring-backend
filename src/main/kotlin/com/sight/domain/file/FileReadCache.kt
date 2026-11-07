package com.sight.domain.file

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.Duration
import java.time.Instant

// 파일명·MIME·제공 정책이 같은 읽기 주소만 재사용한다. 정책 변경 시 무효화한다.
@Embeddable
class FileReadCache(
    readUrl: String? = null,
    readUrlExpiresAt: Instant? = null,
) {
    @Column(name = "read_url", columnDefinition = "TEXT")
    var readUrl: String? = readUrl
        private set

    @Column(name = "read_url_expires_at", columnDefinition = "TIMESTAMP(6)")
    var readUrlExpiresAt: Instant? = readUrlExpiresAt
        private set

    init {
        require((readUrl == null) == (readUrlExpiresAt == null)) { "읽기 주소와 만료 시각은 함께 있어야 합니다." }
        require(readUrl == null || readUrl.isNotBlank()) { "읽기 주소가 필요합니다." }
    }

    fun reusableUrl(at: Instant): String? = readUrl?.takeIf { readUrlExpiresAt?.isAfter(at.plus(REFRESH_MARGIN)) == true }

    fun refresh(
        url: String,
        expiresAt: Instant,
        at: Instant,
    ) {
        require(url.isNotBlank()) { "읽기 주소가 필요합니다." }
        require(expiresAt.isAfter(at)) { "읽기 주소의 만료 시각은 발급 시각보다 늦어야 합니다." }
        readUrl = url
        readUrlExpiresAt = expiresAt
    }

    fun invalidate() {
        readUrl = null
        readUrlExpiresAt = null
    }

    companion object {
        val REFRESH_MARGIN: Duration = Duration.ofMinutes(1)
    }
}
