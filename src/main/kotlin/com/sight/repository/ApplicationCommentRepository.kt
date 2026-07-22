package com.sight.repository

import com.sight.domain.application.ApplicationComment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ApplicationCommentRepository : JpaRepository<ApplicationComment, String> {
    fun findAllByApplicationFormId(applicationFormId: String): List<ApplicationComment>
}
