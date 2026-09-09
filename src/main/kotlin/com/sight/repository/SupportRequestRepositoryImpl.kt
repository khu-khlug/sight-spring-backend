package com.sight.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import com.sight.domain.supportrequest.QSupportRequest
import com.sight.domain.supportrequest.SupportRequest
import com.sight.domain.supportrequest.SupportRequestCategory
import org.springframework.stereotype.Repository

@Repository
class SupportRequestRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : SupportRequestRepositoryCustom {
    private val supportRequest = QSupportRequest.supportRequest

    override fun findSupportRequests(
        offset: Int,
        limit: Int,
        category: SupportRequestCategory?,
    ): List<SupportRequest> =
        queryFactory
            .selectFrom(supportRequest)
            .where(categoryCondition(category))
            .orderBy(supportRequest.createdAt.desc())
            .offset(offset.toLong())
            .limit(limit.toLong())
            .fetch()

    override fun countSupportRequests(category: SupportRequestCategory?): Long =
        queryFactory
            .select(supportRequest.count())
            .from(supportRequest)
            .where(categoryCondition(category))
            .fetchOne() ?: 0L

    private fun categoryCondition(category: SupportRequestCategory?): BooleanExpression? = category?.let(supportRequest.category::eq)
}
