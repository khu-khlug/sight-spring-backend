package com.sight.domain.github

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import java.time.Duration
import java.time.Instant

// 저장소 조회에 회원 개인의 GitHub API 사용량을 쓸지 정한 회원 전역 설정. 모든 그룹에 같게 적용한다.
// 설정한 적 없는 회원은 행이 없으며 사용하지 않는 것(false)으로 본다.
@Entity
@Table(
    name = "github_usage_preference",
    uniqueConstraints = [UniqueConstraint(columnNames = ["member_id"])],
)
class GithubUsagePreference(
    memberId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0

    // 설정한 회원. 회원당 한 행만 둔다.
    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    // 개인 GitHub API 사용량 사용 여부. 켜져 있으면 공용 캐시를 쓰지 않고 개인 인증으로 직접 조회한다.
    @Column(name = "use_personal_github_usage", nullable = false)
    var usePersonalGithubUsage: Boolean = false
        private set

    // 마지막으로 사용을 끈 일시. 이로부터 7일이 지나야 다시 켤 수 있으며, 같은 값(false) 재요청으로는 갱신하지 않는다.
    @Column(name = "last_disabled_at")
    var lastDisabledAt: Instant? = null
        private set

    // JPA 낙관적 잠금용 수정 버전. 동시 변경 충돌을 저장 시 감지한다.
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        private set

    init {
        require(memberId > 0) { "회원 식별자가 올바르지 않습니다." }
    }

    fun canEnable(at: Instant): Boolean {
        val disabledAt = lastDisabledAt ?: return true
        return !at.isBefore(disabledAt.plus(REENABLE_WAITING_PERIOD))
    }

    // 사용 여부를 바꾼다. 같은 값이면 false를 반환한다.
    fun changeUsage(
        usePersonalGithubUsage: Boolean,
        at: Instant,
    ): Boolean {
        if (this.usePersonalGithubUsage == usePersonalGithubUsage) return false
        if (usePersonalGithubUsage) {
            require(canEnable(at)) { "개인 사용량을 끈 뒤 7일이 지나야 다시 켤 수 있습니다." }
        } else {
            lastDisabledAt = at
        }
        this.usePersonalGithubUsage = usePersonalGithubUsage
        return true
    }

    companion object {
        val REENABLE_WAITING_PERIOD: Duration = Duration.ofDays(7)
    }
}
