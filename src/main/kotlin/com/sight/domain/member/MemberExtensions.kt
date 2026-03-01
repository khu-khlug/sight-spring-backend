package com.sight.domain.member

import com.sight.util.UnivPeriod
import com.sight.util.UnivPeriodType
import java.time.LocalDate
import java.time.ZoneId

private val KST = ZoneId.of("Asia/Seoul")

/**
 * INFO21 재인증이 필요한 회원인지 확인합니다.
 * 재학/휴학 상태이고 정지 중이 아닌 회원이 이번 학기에 인증하지 않은 경우 true를 반환합니다.
 */
fun Member.needAuth(): Boolean {
    // 재인증 대상: 재학생 또는 휴학생이면서 현재 정지(탈퇴) 처리되지 않은 회원
    // 졸업생, 제명자 등 나머지 상태는 재인증 의무 없음
    val isTarget =
        (studentStatus == StudentStatus.ABSENCE || studentStatus == StudentStatus.UNDERGRADUATE) &&
            returnAt == null

    if (!isTarget) return false

    val today = LocalDate.now(KST)
    val todayMmdd = today.monthValue * 100 + today.dayOfMonth

    // 학사 연도 기준: 3월 2일부터 새 학년 시작으로 간주
    // 1~3월 1일은 직전 연도의 2학기에 속하므로, 학사 연도를 1년 앞당김
    val currentYear = if (todayMmdd < 302) today.year - 1 else today.year

    // 학기 구분: 3월 2일 ~ 8월 31일은 1학기, 그 외(9월 ~ 다음해 3월 1일)는 2학기
    val currentSemester = if (todayMmdd in 302..831) 1 else 2

    val authDate = khuisauthAt.atZone(KST).toLocalDate()
    val authMmdd = authDate.monthValue * 100 + authDate.dayOfMonth

    // 인증일의 학사 연도 기준: 2월 20일부터 새 학년 1학기 인증 유효 기간으로 간주
    // (개강 전 약 1~2주 정도의 사전 인증 기간을 허용)
    val lastAuthYear = if (authMmdd < 220) authDate.year - 1 else authDate.year

    // 인증일의 학기 구분: 2월 20일 ~ 8월 19일은 1학기 인증, 그 외는 2학기 인증
    // (2학기 개강 전 사전 인증 기간을 마찬가지로 허용)
    val lastAuthSemester = if (authMmdd in 220..819) 1 else 2

    // 마지막 인증 학기가 현재 학기 이상이면 이번 학기 인증을 완료한 것으로 판단
    val authedInThisSemester =
        lastAuthYear > currentYear ||
            (lastAuthYear == currentYear && lastAuthSemester >= currentSemester)

    return !authedInThisSemester
}

/**
 * 회비 납부 대상 여부를 확인합니다.
 * @see 회비에 관한 세부 회칙 제2조, 제5조
 */
fun Member.needPayFee(): Boolean {
    // 정지(탈퇴) 처리된 회원은 납부 대상 아님
    if (returnAt != null) return false
    // 재학생만 납부 의무가 있음 (휴학생, 졸업생 등은 제외)
    if (studentStatus != StudentStatus.UNDERGRADUATE) return false
    // 운영진은 회비 납부 면제
    if (manager) return false

    val joinedDate = createdAt.atZone(KST).toLocalDate()
    val joinedPeriod = UnivPeriod.fromDate(joinedDate)
    val thisTerm = UnivPeriod.fromDate(LocalDate.now(KST)).toTerm()

    // 회칙에 따른 최소 납부 시작 학기 계산
    // - 방학 중 가입: 가입 학기의 다다음 학기부터 납부
    //   (예: 1학기 방학에 가입 → 2학기 건너뜀 → 다음해 1학기부터 납부)
    // - 학기 중 가입: 가입 학기의 다음 학기부터 납부
    //   (예: 1학기 중에 가입 → 2학기부터 납부)
    val leastNeedPayTerm =
        if (joinedPeriod.inVacation()) {
            joinedPeriod.toTerm().next().next()
        } else {
            joinedPeriod.toTerm().next()
        }

    // 현재 학기가 최소 납부 시작 학기를 아직 지나지 않았으면 납부 대상
    val passedMinNeedPayFee = thisTerm.isAfter(leastNeedPayTerm)
    // 4학년 이상(grade >= 4)은 회비 면제이므로, 4학년 미만인 경우에만 납부 대상
    val isGradeLessThanFour = grade < 4

    // 최소 납부 기간을 아직 지나지 않았거나, 4학년 미만이면 납부 대상
    return !passedMinNeedPayFee || isGradeLessThanFour
}

/**
 * 반액 회비 납부 대상 여부를 확인합니다.
 * @see 회비에 관한 세부 회칙 제5조
 */
fun Member.needPayHalfFee(): Boolean {
    // 전액 납부 대상이 아니면 반액도 해당 없음
    if (!needPayFee()) return false

    val joinedDate = createdAt.atZone(KST).toLocalDate()
    val joinedPeriod = UnivPeriod.fromDate(joinedDate)

    // 기말고사 기간 이후 가입 여부 확인
    // 회칙에 따라, 중간고사 이후(기말고사 기간)에 가입한 경우 첫 학기 회비를 반액만 납부
    val joinedAfterMidterm =
        joinedPeriod.type == UnivPeriodType.FIRST_SEMESTER_FINAL_EXAM ||
            joinedPeriod.type == UnivPeriodType.SECOND_SEMESTER_FINAL_EXAM

    val nowTerm = UnivPeriod.fromDate(LocalDate.now(KST)).toTerm()
    val joinedTerm = joinedPeriod.toTerm()

    // 기말고사 이후에 가입했고, 현재 학기가 가입 학기와 동일한 경우에만 반액 납부 대상
    // (첫 납부 학기 한정이므로, 다음 학기부터는 전액 납부)
    return joinedAfterMidterm && nowTerm.isSame(joinedTerm)
}
