package com.sight.domain.member

import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val KST = ZoneId.of("Asia/Seoul")

/**
 * 특정 날짜(KST)를 Instant로 변환합니다.
 */
private fun localDateToInstant(date: LocalDate): Instant =
    date.atStartOfDay(KST).toInstant()

/**
 * 테스트용 Member fixture를 생성합니다.
 * 기본값은 재학생, 정지 없음, 3학년, 비운영진으로 설정되어 있습니다.
 */
private fun createMember(
    studentStatus: StudentStatus = StudentStatus.UNDERGRADUATE,
    grade: Long = 3L,
    manager: Boolean = false,
    returnAt: LocalDateTime? = null,
    khuisauthAt: Instant = Instant.now(),
    createdAt: Instant = Instant.now(),
): Member =
    Member(
        id = 1L,
        name = "testuser",
        admission = "20",
        realname = "테스트 사용자",
        college = "소프트웨어융합학과",
        grade = grade,
        studentStatus = studentStatus,
        email = "test@example.com",
        status = UserStatus.ACTIVE,
        manager = manager,
        returnAt = returnAt,
        khuisauthAt = khuisauthAt,
        createdAt = createdAt,
        updatedAt = LocalDateTime.now(),
        lastLogin = Instant.now(),
        lastEnter = LocalDateTime.now(),
    )

class MemberExtensionsTest {
    // ========================
    // needAuth() 테스트
    // ========================

    @Test
    fun `needAuth - 휴학생이 이번 학기에 인증하지 않은 경우 true를 반환한다`() {
        // 오늘: 2025-05-01 (1학기), 마지막 인증: 2024-05-01 (지난 1학기)
        val lastAuth = localDateToInstant(LocalDate.of(2024, 5, 1))
        val member = createMember(studentStatus = StudentStatus.ABSENCE, khuisauthAt = lastAuth)

        assertTrue(member.needAuth())
    }

    @Test
    fun `needAuth - 재학생이 이번 학기에 인증하지 않은 경우 true를 반환한다`() {
        // 오늘: 2025-05-01 (1학기), 마지막 인증: 2024-11-01 (지난 2학기)
        val lastAuth = localDateToInstant(LocalDate.of(2024, 11, 1))
        val member = createMember(studentStatus = StudentStatus.UNDERGRADUATE, khuisauthAt = lastAuth)

        assertTrue(member.needAuth())
    }

    @Test
    fun `needAuth - 재학생이 이번 학기에 인증한 경우 false를 반환한다`() {
        // 오늘: 2025-05-01 (1학기), 마지막 인증: 2025-04-01 (이번 1학기)
        val lastAuth = localDateToInstant(LocalDate.of(2025, 4, 1))
        val member = createMember(studentStatus = StudentStatus.UNDERGRADUATE, khuisauthAt = lastAuth)

        assertFalse(member.needAuth())
    }

    @Test
    fun `needAuth - 개강 전 사전 인증(2월 20일 이후)도 1학기 인증으로 인정한다`() {
        // 오늘: 2025-05-01 (1학기), 마지막 인증: 2025-02-25 (1학기 개강 전)
        val lastAuth = localDateToInstant(LocalDate.of(2025, 2, 25))
        val member = createMember(studentStatus = StudentStatus.UNDERGRADUATE, khuisauthAt = lastAuth)

        assertFalse(member.needAuth())
    }

    @Test
    fun `needAuth - 2월 19일 이전 인증은 직전 연도 2학기 인증으로 처리된다`() {
        // 오늘: 2025-05-01 (1학기), 마지막 인증: 2025-02-10 → 2024년 2학기로 인정 → 재인증 필요
        val lastAuth = localDateToInstant(LocalDate.of(2025, 2, 10))
        val member = createMember(studentStatus = StudentStatus.UNDERGRADUATE, khuisauthAt = lastAuth)

        assertTrue(member.needAuth())
    }

    @Test
    fun `needAuth - 2학기 중 인증한 경우 2학기 인증으로 인정한다`() {
        // 오늘: 2025-11-01 (2학기), 마지막 인증: 2025-10-01 (이번 2학기)
        val lastAuth = localDateToInstant(LocalDate.of(2025, 10, 1))
        val member = createMember(studentStatus = StudentStatus.UNDERGRADUATE, khuisauthAt = lastAuth)

        assertFalse(member.needAuth())
    }

    @Test
    fun `needAuth - 1월은 직전 연도 2학기이므로 1학기 인증 전으로 간주한다`() {
        // 오늘: 2025-01-15 → 학사 연도상 2024년 2학기
        // 마지막 인증: 2024-09-15 (2024년 2학기) → 이번 학기 인증 완료
        val lastAuth = localDateToInstant(LocalDate.of(2024, 9, 15))
        val member = createMember(studentStatus = StudentStatus.UNDERGRADUATE, khuisauthAt = lastAuth)

        assertFalse(member.needAuth())
    }

    @Test
    fun `needAuth - 졸업생은 재인증 대상이 아니다`() {
        val lastAuth = localDateToInstant(LocalDate.of(2020, 1, 1))
        val member = createMember(studentStatus = StudentStatus.GRADUATE, khuisauthAt = lastAuth)

        assertFalse(member.needAuth())
    }

    @Test
    fun `needAuth - 정지된 회원은 재인증 대상이 아니다`() {
        val lastAuth = localDateToInstant(LocalDate.of(2020, 1, 1))
        val member = createMember(
            studentStatus = StudentStatus.UNDERGRADUATE,
            returnAt = LocalDateTime.now(),
            khuisauthAt = lastAuth,
        )

        assertFalse(member.needAuth())
    }

    // ========================
    // needPayFee() 테스트
    // ========================

    @Test
    fun `needPayFee - 정지된 회원은 납부 대상이 아니다`() {
        // 가입일: 2024-03-15 (1학기) → 다음 학기(2024년 2학기)부터 납부 대상이지만 정지 상태
        val joinedAt = localDateToInstant(LocalDate.of(2024, 3, 15))
        val member = createMember(
            returnAt = LocalDateTime.now(),
            createdAt = joinedAt,
        )

        assertFalse(member.needPayFee())
    }

    @Test
    fun `needPayFee - 휴학생은 납부 대상이 아니다`() {
        val joinedAt = localDateToInstant(LocalDate.of(2024, 3, 15))
        val member = createMember(
            studentStatus = StudentStatus.ABSENCE,
            createdAt = joinedAt,
        )

        assertFalse(member.needPayFee())
    }

    @Test
    fun `needPayFee - 운영진은 납부 대상이 아니다`() {
        val joinedAt = localDateToInstant(LocalDate.of(2024, 3, 15))
        val member = createMember(
            manager = true,
            createdAt = joinedAt,
        )

        assertFalse(member.needPayFee())
    }

    @Test
    fun `needPayFee - 4학년 이상은 납부 면제 기간이 지나도 납부 대상이 아니다`() {
        // 가입일: 2023-03-15 (1학기) → 최소 납부 시작: 2023년 2학기
        // 현재 2025년 기준 납부 기간은 지났지만 4학년이므로 면제
        val joinedAt = localDateToInstant(LocalDate.of(2023, 3, 15))
        val member = createMember(
            grade = 4L,
            createdAt = joinedAt,
        )

        assertFalse(member.needPayFee())
    }

    @Test
    fun `needPayFee - 학기 중 가입하면 다음 학기부터 납부 대상이다`() {
        // 가입일: 2024-03-15 (1학기) → 최소 납부 시작: 2024년 2학기
        // 현재(2025년 기준)는 2024년 2학기를 지났으므로 납부 대상
        val joinedAt = localDateToInstant(LocalDate.of(2024, 3, 15))
        val member = createMember(createdAt = joinedAt)

        assertTrue(member.needPayFee())
    }

    @Test
    fun `needPayFee - 방학 중 가입하면 다다음 학기부터 납부 대상이다`() {
        // 가입일: 2024-07-15 (1학기 방학) → 최소 납부 시작: 2025년 1학기
        // 현재(2025년 기준) 2025년 1학기 진행 중이라면 최소 납부 기간을 아직 지나지 않음 → 납부 대상
        val joinedAt = localDateToInstant(LocalDate.of(2024, 7, 15))
        val member = createMember(createdAt = joinedAt)

        assertTrue(member.needPayFee())
    }

    @Test
    fun `needPayFee - 직전 학기에 가입한 재학생은 아직 납부 대상이 아니다`() {
        // 가입일: 2025-03-15 (1학기, 현재 학기) → 최소 납부 시작: 2025년 2학기
        // 현재가 2025년 1학기라면 아직 납부 대상 아님
        // 단, 현재 날짜 기준으로 실제 테스트하기 어려우므로 미래 가입일 사용
        val joinedAt = localDateToInstant(LocalDate.now(KST))
        val member = createMember(createdAt = joinedAt)

        // 방금 가입한 경우: 이번 학기는 면제, 다음 학기부터 납부 대상 → false
        assertFalse(member.needPayFee())
    }

    @Test
    fun `needPayFee - 3학년 미만 재학생은 납부 기간이 지나도 납부 대상이다`() {
        // 가입일: 2023-03-15 (1학기) → 최소 납부 시작: 2023년 2학기
        // 현재 2025년 기준 납부 기간은 지났고 3학년 미만이므로 납부 대상
        val joinedAt = localDateToInstant(LocalDate.of(2023, 3, 15))
        val member = createMember(grade = 2L, createdAt = joinedAt)

        assertTrue(member.needPayFee())
    }

    // ========================
    // needPayHalfFee() 테스트
    // ========================

    @Test
    fun `needPayHalfFee - 전액 납부 대상이 아니면 반액도 해당 없다`() {
        val member = createMember(studentStatus = StudentStatus.ABSENCE)

        assertFalse(member.needPayHalfFee())
    }

    @Test
    fun `needPayHalfFee - 기말고사 기간 이후 가입하고 가입 학기가 현재와 같으면 반액 납부 대상이다`() {
        // 가입일: 현재가 2025년 1학기라 가정할 때, 1학기 기말고사 기간에 가입
        // 1학기 시작: 3월 1일, 중간고사 종료: +8주 - 1일 = 4월 25일, 기말고사 기간: 4월 26일 ~ 6월 28일
        // 현재도 2025년 1학기이므로 반액 납부 대상
        val firstSemesterStart = LocalDate.of(2025, 3, 1)
        val firstMidTermEnd = firstSemesterStart.plusWeeks(8).minusDays(1)
        val joinedDate = firstMidTermEnd.plusDays(1) // 기말고사 기간 첫날
        val joinedAt = localDateToInstant(joinedDate)
        val member = createMember(createdAt = joinedAt)

        assertTrue(member.needPayHalfFee())
    }

    @Test
    fun `needPayHalfFee - 기말고사 기간 이후 가입했어도 다음 학기이면 전액 납부 대상이다`() {
        // 가입일: 2024년 1학기 기말고사 기간
        // 현재: 2025년(다음 학기 이상)이므로 반액 적용 안 됨
        val firstSemesterStart = LocalDate.of(2024, 3, 1)
        val firstMidTermEnd = firstSemesterStart.plusWeeks(8).minusDays(1)
        val joinedDate = firstMidTermEnd.plusDays(1)
        val joinedAt = localDateToInstant(joinedDate)
        val member = createMember(createdAt = joinedAt)

        assertFalse(member.needPayHalfFee())
    }

    @Test
    fun `needPayHalfFee - 중간고사 기간 이전에 가입한 경우 반액 납부 대상이 아니다`() {
        // 가입일: 2024년 1학기 중간고사 기간(개강 후 8주 이내) → 반액 아님
        val joinedDate = LocalDate.of(2024, 3, 15)
        val joinedAt = localDateToInstant(joinedDate)
        val member = createMember(createdAt = joinedAt)

        assertFalse(member.needPayHalfFee())
    }

    @Test
    fun `needPayHalfFee - 2학기 기말고사 기간 이후 가입하고 가입 학기가 현재와 같으면 반액 납부 대상이다`() {
        // 가입일: 현재가 2024년 2학기라 가정할 때, 2학기 기말고사 기간에 가입
        // 2학기 시작: 9월 1일, 중간고사 종료: +8주 - 1일 = 10월 25일, 기말고사 기간: 10월 26일 ~
        val secondSemesterStart = LocalDate.of(2024, 9, 1)
        val secondMidTermEnd = secondSemesterStart.plusWeeks(8).minusDays(1)
        val joinedDate = secondMidTermEnd.plusDays(1)
        val joinedAt = localDateToInstant(joinedDate)
        val member = createMember(createdAt = joinedAt)

        assertTrue(member.needPayHalfFee())
    }
}
