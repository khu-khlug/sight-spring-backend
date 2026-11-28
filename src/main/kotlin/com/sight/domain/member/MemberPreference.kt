package com.sight.domain.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Duration
import java.time.Instant

// 회원 전역 설정. 사이트 전체 기능에 같게 적용한다.
// 설정한 적 없는 회원은 행이 없으며 각 설정의 기본값으로 본다.
@Entity
// 유니크(member_id): 회원당 설정 행 하나.
@Table(
    name = "member_preference",
    uniqueConstraints = [UniqueConstraint(columnNames = ["member_id"])],
)
class MemberPreference(
    id: Long,
    memberId: Long,
) {
    @Id
    @Column(name = "id", nullable = false)
    val id: Long = id

    // 설정한 회원. 회원당 한 행만 둔다.
    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    // 저장소 조회에 개인 GitHub API 사용량을 쓸지 여부. 켜져 있으면 공용 캐시를 쓰지 않고 개인 인증으로 직접 조회한다. 기본값은 false다.
    @Column(name = "use_personal_github_usage", nullable = false)
    var usePersonalGithubUsage: Boolean = false
        private set

    // 개인 GitHub API 사용량을 마지막으로 끈 일시. 이로부터 7일이 지나야 다시 켤 수 있으며, 같은 값(false) 재요청으로는 갱신하지 않는다.
    @Column(name = "github_usage_last_disabled_at")
    var githubUsageLastDisabledAt: Instant? = null
        private set

    init {
        require(id > 0) { "회원 설정 식별자가 올바르지 않습니다." }
        require(memberId > 0) { "회원 식별자가 올바르지 않습니다." }
    }

    fun canEnableGithubUsage(at: Instant): Boolean {
        val disabledAt = githubUsageLastDisabledAt ?: return true
        return !at.isBefore(disabledAt.plus(GITHUB_USAGE_REENABLE_WAITING_PERIOD))
    }

    // 개인 GitHub API 사용량 사용 여부를 바꾼다. 같은 값이면 false를 반환한다.
    fun changeGithubUsage(
        usePersonalGithubUsage: Boolean,
        at: Instant,
    ): Boolean {
        if (this.usePersonalGithubUsage == usePersonalGithubUsage) return false
        if (usePersonalGithubUsage) {
            require(canEnableGithubUsage(at)) { "개인 사용량을 끈 뒤 7일이 지나야 다시 켤 수 있습니다." }
        } else {
            githubUsageLastDisabledAt = at
        }
        this.usePersonalGithubUsage = usePersonalGithubUsage
        return true
    }

    companion object {
        val GITHUB_USAGE_REENABLE_WAITING_PERIOD: Duration = Duration.ofDays(7)
    }
}
