package com.sight.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.fee.QFeeHistory
import com.sight.domain.member.Member
import com.sight.domain.member.MemberTagFilter
import com.sight.domain.member.QMember
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.util.UnivPeriod
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@Repository
class MemberRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : MemberRepositoryCustom {
    private val member = QMember.member
    private val feeHistory = QFeeHistory.feeHistory
    private val kst = ZoneId.of("Asia/Seoul")

    override fun findMembers(
        email: String?,
        phone: String?,
        name: String?,
        number: String?,
        college: String?,
        grade: Int?,
        studentStatus: StudentStatus?,
        tag: MemberTagFilter?,
        limit: Int,
        offset: Int,
    ): List<Member> =
        queryFactory
            .selectFrom(member)
            .where(*baseConditions(), *filterConditions(email, phone, name, number, college, grade, studentStatus), tagCondition(tag))
            .orderBy(member.realname.asc())
            .offset(offset.toLong())
            .limit(limit.toLong())
            .fetch()

    override fun countMembers(
        email: String?,
        phone: String?,
        name: String?,
        number: String?,
        college: String?,
        grade: Int?,
        studentStatus: StudentStatus?,
        tag: MemberTagFilter?,
    ): Long =
        queryFactory
            .select(member.count())
            .from(member)
            .where(*baseConditions(), *filterConditions(email, phone, name, number, college, grade, studentStatus), tagCondition(tag))
            .fetchOne() ?: 0L

    // 항상 적용되는 고정 필터
    // active != 0 → status != UNAUTHORIZED (탈퇴자 제외)
    // state != -1 → studentStatus != UNITED (교류생 제외)
    private fun baseConditions(): Array<BooleanExpression> =
        arrayOf(
            member.status.ne(UserStatus.UNAUTHORIZED),
            member.studentStatus.ne(StudentStatus.UNITED),
        )

    private fun filterConditions(
        email: String?,
        phone: String?,
        name: String?,
        number: String?,
        college: String?,
        grade: Int?,
        studentStatus: StudentStatus?,
    ): Array<BooleanExpression?> =
        arrayOf(
            email?.let { member.email.like("%$it%") },
            phone?.let { member.phone.like("%$it%") },
            name?.let { member.realname.like("%$it%") },
            number?.let { member.number.stringValue().like("%$it%") },
            college?.let { member.college.like("%$it%") },
            grade?.let { member.grade.eq(it.toLong()) },
            studentStatus?.let { member.studentStatus.eq(it) },
        )

    private fun tagCondition(tag: MemberTagFilter?): BooleanExpression? =
        when (tag) {
            MemberTagFilter.BLOCKED -> member.status.eq(UserStatus.INACTIVE)
            MemberTagFilter.MINUS_EXP -> member.expoint.lt(0L)
            MemberTagFilter.UNAUTHORIZED -> unauthorizedCondition()
            MemberTagFilter.FEE_TARGET -> feeCondition(halfFee = false)
            MemberTagFilter.HALF_FEE_TARGET -> feeCondition(halfFee = true)
            null -> null
        }

    /**
     * 미인증 조건:
     * - 재학생 또는 휴학생이면서 정지 중이 아닌 회원
     * - 이번 학기 인증 기준일 이전에 마지막으로 인증한 경우
     *
     * 인증 기준일:
     * - 1학기: 당해 2월 20일 00:00 KST
     * - 2학기: 당해 8월 20일 00:00 KST
     */
    private fun unauthorizedCondition(): BooleanExpression {
        val today = LocalDate.now(kst)
        val thisTerm = UnivPeriod.fromDate(today).toTerm()

        val authThreshold: Instant =
            if (thisTerm.semester == 1) {
                LocalDate.of(thisTerm.year, 2, 20).atTime(LocalTime.MIDNIGHT).toInstant(ZoneOffset.ofHours(9))
            } else {
                LocalDate.of(thisTerm.year, 8, 20).atTime(LocalTime.MIDNIGHT).toInstant(ZoneOffset.ofHours(9))
            }

        return member.studentStatus
            .`in`(StudentStatus.ABSENCE, StudentStatus.UNDERGRADUATE)
            .and(member.returnAt.isNull)
            .and(member.khuisauthAt.before(authThreshold))
    }

    /**
     * 납부 대상 / 반액 납부 대상 조건.
     *
     * 공통 조건 (needPayFee):
     * - returnAt IS NULL
     * - studentStatus = UNDERGRADUATE
     * - manager = false
     * - grade < 4 OR createdAt >= [이번 학기 납부 기준 시작일]
     *
     * 납부 기준 시작일: needPayFee 로직에서 leastNeedPayTerm >= thisTerm 이 되는 최소 가입일
     * - thisTerm이 Y년 1학기: Y-1년 여름방학 시작일 (firstFinalEnd + 1일 of Y-1)
     * - thisTerm이 Y년 2학기: Y년 9월 1일
     *
     * halfFee=false (납부 대상): createdAt 이 현재 학기 기말고사 기간 밖
     * halfFee=true  (반액 납부 대상): createdAt 이 현재 학기 기말고사 기간 안
     */
    private fun feeCondition(halfFee: Boolean): BooleanExpression {
        val today = LocalDate.now(kst)
        val thisTerm = UnivPeriod.fromDate(today).toTerm()

        // 납부 기준 시작일: 이 날짜 이후에 가입한 회원은 "아직 최소 납부 시작 학기를 지나지 않음"
        val needPayThresholdDate: LocalDate =
            if (thisTerm.semester == 1) {
                // Y-1년 1학기 firstFinalEnd = March 1(Y-1) + 16주 - 1일
                val firstStart = LocalDate.of(thisTerm.year - 1, 3, 1)
                firstStart.plusWeeks(16) // firstFinalEnd + 1일
            } else {
                LocalDate.of(thisTerm.year, 9, 1)
            }
        val needPayThreshold = needPayThresholdDate.atTime(LocalTime.MIDNIGHT).toInstant(ZoneOffset.ofHours(9))

        // 현재 학기 기말고사 기간
        val (finalExamStart, finalExamEnd) =
            if (thisTerm.semester == 1) {
                val firstStart = LocalDate.of(thisTerm.year, 3, 1)
                firstStart.plusWeeks(8) to firstStart.plusWeeks(16).minusDays(1)
            } else {
                val secondStart = LocalDate.of(thisTerm.year, 9, 1)
                secondStart.plusWeeks(8) to secondStart.plusWeeks(16).minusDays(1)
            }
        val finalExamStartInstant = finalExamStart.atTime(LocalTime.MIDNIGHT).toInstant(ZoneOffset.ofHours(9))
        val finalExamEndInstant = finalExamEnd.atTime(LocalTime.MAX).toInstant(ZoneOffset.ofHours(9))

        // 이번 학기 이미 납부한 회원 제외 (서브쿼리)
        val notPaidSubquery =
            JPAExpressions
                .selectOne()
                .from(feeHistory)
                .where(
                    feeHistory.userId.eq(member.id),
                    feeHistory.year.eq(thisTerm.year),
                    feeHistory.semester.eq(thisTerm.semester),
                ).notExists()

        val feeTargetCondition =
            member.returnAt.isNull
                .and(member.studentStatus.eq(StudentStatus.UNDERGRADUATE))
                .and(member.manager.isFalse)
                .and(
                    member.grade.lt(4L)
                        .or(member.createdAt.goe(needPayThreshold)),
                )

        val halfFeeCondition =
            member.createdAt.goe(finalExamStartInstant).and(member.createdAt.loe(finalExamEndInstant))

        return if (halfFee) {
            feeTargetCondition.and(halfFeeCondition).and(notPaidSubquery)
        } else {
            feeTargetCondition.and(halfFeeCondition.not()).and(notPaidSubquery)
        }
    }
}
