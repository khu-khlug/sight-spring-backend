package com.sight.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class OffsetLimitPageable(
    private val offset: Int,
    private val limit: Int,
    private val sort: Sort,
) : Pageable {
    override fun getPageNumber(): Int = offset / limit

    override fun getPageSize(): Int = limit

    override fun getOffset(): Long = offset.toLong()

    override fun getSort(): Sort = sort

    override fun next(): Pageable = OffsetLimitPageable(offset + limit, limit, sort)

    override fun previousOrFirst(): Pageable = first()

    override fun first(): Pageable = OffsetLimitPageable(0, limit, sort)

    override fun withPage(pageNumber: Int): Pageable = OffsetLimitPageable(pageNumber * limit, limit, sort)

    override fun hasPrevious(): Boolean = offset > 0
}
