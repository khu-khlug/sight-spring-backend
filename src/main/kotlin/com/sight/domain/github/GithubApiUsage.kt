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

@Entity
@Table(
    name = "github_api_usage",
    uniqueConstraints = [UniqueConstraint(columnNames = ["member_id"])],
)
class GithubApiUsage(
    memberId: Long,
    countStartAt: Instant,
) {
    // 사용량 행의 DB 식별자. 회원 식별자와 구분해 해당 행을 조회·갱신하는 기본 키다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0

    // 사용량을 집계할 회원. 그룹·저장소와 관계없이 합산하며 회원당 최대 한 행만 둔다.
    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    // 현재 사용량 집계를 시작한 시각. 첫 호출 예약 시각을 저장하고, 시작부터 1시간이 지나면 새 집계를 시작한다.
    // 미사용 회원은 null 시각의 행을 미리 만들지 않고 사용량 행 자체를 만들지 않는다.
    @Column(name = "count_start_at", nullable = false)
    var countStartAt: Instant = countStartAt
        private set

    // 현재 집계 기간에 GitHub 기본 호출 한도를 실제로 소모한 횟수. SIGHT API 요청 수와는 다르다.
    @Column(name = "used_count", nullable = false)
    var usedCount: Int = 0
        private set

    // 외부 호출 전에 확보했고 실제 사용량 반영 여부가 미확정인 횟수. 동시 호출의 한도 초과를 막는다.
    // 한도 소모 시 usedCount로 옮기고, 변경 없음(304)이나 미전송이면 이 값만 줄인다.
    @Column(name = "pending_count", nullable = false)
    var pendingCount: Int = 0
        private set

    // JPA 낙관적 잠금용 수정 버전. 동시에 같은 사용량 행을 갱신할 때 증가분이 유실되는 충돌을 감지한다.
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        private set

    init {
        require(memberId > 0) { "회원 식별자가 올바르지 않습니다." }
    }

    // 이미 사용했거나 처리 대기 중인 횟수를 뺀 추가 예약 가능량. DB에는 별도 저장하지 않는다.
    val remainingCount: Int
        get() = HOURLY_LIMIT - usedCount - pendingCount

    fun reserve(
        at: Instant,
        count: Int = 1,
    ) {
        require(count in 1..HOURLY_LIMIT) { "예약 횟수는 1 이상 시간당 한도 이하여야 합니다." }
        require(!at.isBefore(countStartAt)) { "사용량 집계 시작 전에는 예약할 수 없습니다." }
        if (!at.isBefore(countStartAt.plus(Duration.ofHours(1)))) resetCount(at)
        require(count <= remainingCount) { "GitHub API 개인 할당량이 부족합니다." }
        pendingCount += count
    }

    fun resetCount(at: Instant) {
        require(!at.isBefore(countStartAt.plus(Duration.ofHours(1)))) { "사용량 초기화 시각이 되지 않았습니다." }
        require(pendingCount == 0) { "이전 집계 기간의 예약을 먼저 처리해야 합니다." }
        usedCount = 0
        countStartAt = at
    }

    // 실제 기본 호출 한도를 소모했을 때만 예약을 사용량으로 확정한다.
    fun confirmUsage(count: Int = 1) {
        validateReservation(count)
        pendingCount -= count
        usedCount += count
    }

    // 변경 없음 응답 또는 미전송 등 한도를 소모하지 않은 요청의 예약을 해제한다.
    fun releaseReservation(count: Int = 1) {
        validateReservation(count)
        pendingCount -= count
    }

    private fun validateReservation(count: Int) {
        require(count > 0 && count <= pendingCount) { "처리할 예약 횟수가 올바르지 않습니다." }
    }

    companion object {
        // 회원 한 명이 공용 토큰에서 소모할 수 있는 시간당 기본 호출 한도다.
        const val HOURLY_LIMIT: Int = 100

        // 사용량 행이 없는 회원의 첫 외부 호출을 예약할 때만 생성한다.
        fun createWithReservation(
            memberId: Long,
            at: Instant,
            count: Int = 1,
        ): GithubApiUsage =
            GithubApiUsage(
                memberId = memberId,
                countStartAt = at,
            ).apply { reserve(at, count) }
    }
}
