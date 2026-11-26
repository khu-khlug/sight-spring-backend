package com.sight.domain.member

import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MemberPreferenceTest {
    @Test
    fun `비활성화 후 정확히 7일이 지나면 다시 활성화할 수 있다`() {
        val disabledAt = Instant.parse("2026-01-01T00:00:00Z")
        val preference = MemberPreference(id = 1L, memberId = 2L)
        preference.changeGithubUsage(true, disabledAt.minusSeconds(1))
        preference.changeGithubUsage(false, disabledAt)

        assertFalse(preference.canEnableGithubUsage(disabledAt.plus(7, ChronoUnit.DAYS).minusNanos(1)))
        assertTrue(preference.canEnableGithubUsage(disabledAt.plus(7, ChronoUnit.DAYS)))
        assertTrue(preference.changeGithubUsage(true, disabledAt.plus(7, ChronoUnit.DAYS)))
    }

    @Test
    fun `이미 비활성화된 설정을 다시 비활성화해도 대기 시작 시각을 바꾸지 않는다`() {
        val disabledAt = Instant.parse("2026-01-01T00:00:00Z")
        val preference = MemberPreference(id = 1L, memberId = 2L)
        preference.changeGithubUsage(true, disabledAt.minusSeconds(1))
        preference.changeGithubUsage(false, disabledAt)

        assertFalse(preference.changeGithubUsage(false, disabledAt.plus(3, ChronoUnit.DAYS)))
        assertFalse(preference.canEnableGithubUsage(disabledAt.plus(7, ChronoUnit.DAYS).minusNanos(1)))
        assertTrue(preference.canEnableGithubUsage(disabledAt.plus(7, ChronoUnit.DAYS)))
    }
}
