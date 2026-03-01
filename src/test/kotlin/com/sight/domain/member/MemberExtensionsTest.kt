package com.sight.domain.member

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val KST = ZoneId.of("Asia/Seoul")

private fun localDateToInstant(date: LocalDate): Instant = date.atStartOfDay(KST).toInstant()

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
    private lateinit var mockedLocalDate: MockedStatic<LocalDate>

    @BeforeEach
    fun setUp() {
        // LocalDate.of() 등 다른 메서드는 실제 구현을 유지하고, now(ZoneId)만 고정
        mockedLocalDate = Mockito.mockStatic(LocalDate::class.java, Mockito.CALLS_REAL_METHODS)
    }

    @AfterEach
    fun tearDown() {
        mockedLocalDate.close()
    }

    private fun mockToday(date: LocalDate) {
        mockedLocalDate.`when`<LocalDate> { LocalDate.now(any<ZoneId>()) }.thenReturn(date)
    }

    // ================================================================
    // needAuth() 테스트
    // ================================================================

    @Test
    fun `needAuth - 이번 학기에 인증한 재학생은 재인증이 필요 없다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기
        // lastAuth: 2025-03-15 → authMmdd=315, in 220..819 → 2025년 1학기 인증 → 재인증 불필요
        val lastAuth = localDateToInstant(LocalDate.of(2025, 3, 15))
        val member = createMember(khuisauthAt = lastAuth)

        assertFalse(member.needAuth())
    }

    @Test
    fun `needAuth - 이번 학기에 인증하지 않은 재학생은 재인증이 필요하다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기
        // lastAuth: 2024-10-01 → authMmdd=1001, not in 220..819 → 2024년 2학기 인증 → 재인증 필요
        val lastAuth = localDateToInstant(LocalDate.of(2024, 10, 1))
        val member = createMember(khuisauthAt = lastAuth)

        assertTrue(member.needAuth())
    }

    @Test
    fun `needAuth - 1학기 개강 전 사전 인증(2월 20일 이후)도 1학기 인증으로 인정한다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기
        // lastAuth: 2025-02-25 → authMmdd=225, in 220..819 → 2025년 1학기 인증
        // 개강(3월 1일) 이전이지만 1학기 인증으로 인정 → 재인증 불필요
        val lastAuth = localDateToInstant(LocalDate.of(2025, 2, 25))
        val member = createMember(khuisauthAt = lastAuth)

        assertFalse(member.needAuth())
    }

    @Test
    fun `needAuth - 2월 19일 이전 인증은 직전 연도 2학기 인증으로 처리되어 1학기 기준 재인증이 필요하다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기
        // lastAuth: 2025-02-10 → authMmdd=210, < 220 → lastAuthYear=2024(직전 연도), lastAuthSemester=2
        // 2024년 2학기 인증이므로 2025년 1학기 기준으로는 재인증 필요
        val lastAuth = localDateToInstant(LocalDate.of(2025, 2, 10))
        val member = createMember(khuisauthAt = lastAuth)

        assertTrue(member.needAuth())
    }

    @Test
    fun `needAuth - 2학기 개강 전 사전 인증(8월 20일 이후)도 2학기 인증으로 인정한다`() {
        mockToday(LocalDate.of(2025, 11, 15)) // 2025년 2학기
        // lastAuth: 2025-08-25 → authMmdd=825, not in 220..819 → 2025년 2학기 인증
        // 개강(9월 1일) 이전이지만 2학기 인증으로 인정 → 재인증 불필요
        val lastAuth = localDateToInstant(LocalDate.of(2025, 8, 25))
        val member = createMember(khuisauthAt = lastAuth)

        assertFalse(member.needAuth())
    }

    @Test
    fun `needAuth - 8월 19일 이전 인증은 1학기 인증으로 처리되어 2학기 기준 재인증이 필요하다`() {
        mockToday(LocalDate.of(2025, 11, 15)) // 2025년 2학기
        // lastAuth: 2025-08-19 → authMmdd=819, in 220..819 → 2025년 1학기 인증
        // 현재 2025년 2학기이므로 재인증 필요
        val lastAuth = localDateToInstant(LocalDate.of(2025, 8, 19))
        val member = createMember(khuisauthAt = lastAuth)

        assertTrue(member.needAuth())
    }

    @Test
    fun `needAuth - 휴학생도 재인증 대상이다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기
        val lastAuth = localDateToInstant(LocalDate.of(2024, 1, 1))
        val member = createMember(studentStatus = StudentStatus.ABSENCE, khuisauthAt = lastAuth)

        assertTrue(member.needAuth())
    }

    @Test
    fun `needAuth - 졸업생은 재인증 대상이 아니다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기
        val lastAuth = localDateToInstant(LocalDate.of(2020, 1, 1))
        val member = createMember(studentStatus = StudentStatus.GRADUATE, khuisauthAt = lastAuth)

        assertFalse(member.needAuth())
    }

    @Test
    fun `needAuth - 정지된 회원은 재인증 대상이 아니다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기
        val lastAuth = localDateToInstant(LocalDate.of(2020, 1, 1))
        val member = createMember(returnAt = LocalDateTime.now(), khuisauthAt = lastAuth)

        assertFalse(member.needAuth())
    }

    // ================================================================
    // needPayFee() 테스트
    // ================================================================

    @Test
    fun `needPayFee - 정지된 회원은 납부 대상이 아니다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기, UnivTerm(2025, 1)
        val joinedAt = localDateToInstant(LocalDate.of(2024, 3, 15))
        val member = createMember(returnAt = LocalDateTime.now(), createdAt = joinedAt)

        assertFalse(member.needPayFee())
    }

    @Test
    fun `needPayFee - 휴학생은 납부 대상이 아니다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기, UnivTerm(2025, 1)
        val joinedAt = localDateToInstant(LocalDate.of(2024, 3, 15))
        val member = createMember(studentStatus = StudentStatus.ABSENCE, createdAt = joinedAt)

        assertFalse(member.needPayFee())
    }

    @Test
    fun `needPayFee - 운영진은 납부 대상이 아니다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기, UnivTerm(2025, 1)
        val joinedAt = localDateToInstant(LocalDate.of(2024, 3, 15))
        val member = createMember(manager = true, createdAt = joinedAt)

        assertFalse(member.needPayFee())
    }

    @Test
    fun `needPayFee - 학기 중 가입한 재학생은 다음 학기부터 납부 대상이다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기, UnivTerm(2025, 1)
        // 가입: 2024년 1학기(2024-03-15) → leastNeedPayTerm = UnivTerm(2024, 2)
        // 현재: UnivTerm(2025, 1) → isAfter(2024년 2학기) = true → grade=3 → 납부 대상
        val joinedAt = localDateToInstant(LocalDate.of(2024, 3, 15))
        val member = createMember(grade = 3L, createdAt = joinedAt)

        assertTrue(member.needPayFee())
    }

    @Test
    fun `needPayFee - 방학 중 가입한 재학생은 다다음 학기부터 납부 대상이다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기, UnivTerm(2025, 1)
        // 가입: 2024년 1학기 방학(2024-07-15) → leastNeedPayTerm = UnivTerm(2025, 1)
        // 현재: UnivTerm(2025, 1) → isAfter(2025년 1학기) = false → !passedMin = true → 납부 대상
        val joinedAt = localDateToInstant(LocalDate.of(2024, 7, 15))
        val member = createMember(grade = 3L, createdAt = joinedAt)

        assertTrue(member.needPayFee())
    }

    @Test
    fun `needPayFee - 4학년 이상이고 최소 납부 기간을 지난 경우 납부 면제된다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기, UnivTerm(2025, 1)
        // 가입: 2023년 1학기(2023-03-15) → leastNeedPayTerm = UnivTerm(2023, 2)
        // 현재: UnivTerm(2025, 1) → isAfter(2023년 2학기) = true → grade=4 → 면제
        val joinedAt = localDateToInstant(LocalDate.of(2023, 3, 15))
        val member = createMember(grade = 4L, createdAt = joinedAt)

        assertFalse(member.needPayFee())
    }

    @Test
    fun `needPayFee - 4학년이라도 최소 납부 기간을 지나지 않으면 납부 대상이다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기, UnivTerm(2025, 1)
        // 가입: 2024년 2학기(2024-09-15) → leastNeedPayTerm = UnivTerm(2025, 1)
        // 현재: UnivTerm(2025, 1) → isAfter(2025년 1학기) = false → !passedMin = true → 납부 대상
        val joinedAt = localDateToInstant(LocalDate.of(2024, 9, 15))
        val member = createMember(grade = 4L, createdAt = joinedAt)

        assertTrue(member.needPayFee())
    }

    // ================================================================
    // needPayHalfFee() 테스트
    // ================================================================

    @Test
    fun `needPayHalfFee - 전액 납부 대상이 아니면 반액도 해당 없다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기
        val member = createMember(studentStatus = StudentStatus.ABSENCE)

        assertFalse(member.needPayHalfFee())
    }

    @Test
    fun `needPayHalfFee - 중간고사 이전에 가입한 경우 반액 납부 대상이 아니다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기
        // 가입: 2025년 1학기 중간고사 이전(2025-03-15) → FIRST_SEMESTER_MIDTERM_EXAM → joinedAfterMidterm = false
        val joinedAt = localDateToInstant(LocalDate.of(2025, 3, 15))
        val member = createMember(createdAt = joinedAt)

        assertFalse(member.needPayHalfFee())
    }

    @Test
    fun `needPayHalfFee - 기말고사 기간에 가입했어도 다음 학기 이후면 전액 납부 대상이다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기, UnivTerm(2025, 1)
        // 가입: 2024년 1학기 기말고사(2024-04-26) → joinedAfterMidterm = true, joinedTerm = UnivTerm(2024, 1)
        // nowTerm = UnivTerm(2025, 1) ≠ joinedTerm → 반액 미해당
        val joinedAt = localDateToInstant(LocalDate.of(2024, 4, 26))
        val member = createMember(createdAt = joinedAt)

        assertFalse(member.needPayHalfFee())
    }

    @Test
    fun `needPayHalfFee - 1학기 기말고사 기간에 가입하고 가입 학기가 현재와 같으면 반액 납부 대상이다`() {
        mockToday(LocalDate.of(2025, 5, 15)) // 2025년 1학기, UnivTerm(2025, 1)
        // 가입: 2025년 1학기 기말고사(2025-04-26) → FIRST_SEMESTER_FINAL_EXAM → joinedAfterMidterm = true
        // nowTerm = joinedTerm = UnivTerm(2025, 1) → 반액 납부 대상
        val joinedAt = localDateToInstant(LocalDate.of(2025, 4, 26))
        val member = createMember(createdAt = joinedAt)

        assertTrue(member.needPayHalfFee())
    }

    @Test
    fun `needPayHalfFee - 2학기 기말고사 기간에 가입하고 가입 학기가 현재와 같으면 반액 납부 대상이다`() {
        mockToday(LocalDate.of(2025, 11, 15)) // 2025년 2학기, UnivTerm(2025, 2)
        // 가입: 2025년 2학기 기말고사(2025-10-27) → SECOND_SEMESTER_FINAL_EXAM → joinedAfterMidterm = true
        // nowTerm = joinedTerm = UnivTerm(2025, 2) → 반액 납부 대상
        val joinedAt = localDateToInstant(LocalDate.of(2025, 10, 27))
        val member = createMember(createdAt = joinedAt)

        assertTrue(member.needPayHalfFee())
    }
}
