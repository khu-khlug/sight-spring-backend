package com.sight.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.member.Member
import com.sight.domain.member.QMember
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import org.springframework.stereotype.Repository

@Repository
class MemberRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : MemberRepositoryCustom {
    private val member = QMember.member

    override fun findMembers(
        email: String?,
        phone: String?,
        name: String?,
        number: String?,
        college: String?,
        grade: Int?,
        studentStatus: StudentStatus?,
        limit: Int,
        offset: Int,
    ): List<Member> =
        queryFactory
            .selectFrom(member)
            .where(*baseConditions(), *filterConditions(email, phone, name, number, college, grade, studentStatus))
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
    ): Long =
        queryFactory
            .select(member.count())
            .from(member)
            .where(*baseConditions(), *filterConditions(email, phone, name, number, college, grade, studentStatus))
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
}
