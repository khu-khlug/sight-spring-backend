package com.sight.repository

import com.sight.domain.supportrequest.SupportRequest
import com.sight.domain.supportrequest.SupportRequestCategory

interface SupportRequestRepositoryCustom {
    fun findSupportRequests(
        offset: Int,
        limit: Int,
        category: SupportRequestCategory?,
    ): List<SupportRequest>

    fun countSupportRequests(category: SupportRequestCategory?): Long
}
