package com.sight.repository

import com.sight.domain.supportrequest.SupportRequestComment
import org.springframework.data.jpa.repository.JpaRepository

interface SupportRequestCommentRepository : JpaRepository<SupportRequestComment, String> {
    fun existsBySupportRequestId(supportRequestId: String): Boolean

    fun findBySupportRequestIdOrderByCreatedAtAscIdAsc(supportRequestId: String): List<SupportRequestComment>
}
