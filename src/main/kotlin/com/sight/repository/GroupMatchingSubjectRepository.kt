package com.sight.repository

import com.sight.domain.groupmatching.GroupMatchingSubject
import org.springframework.data.jpa.repository.JpaRepository

interface GroupMatchingSubjectRepository : JpaRepository<GroupMatchingSubject, String> {
    fun findAllByAnswerId(answerId: String): List<GroupMatchingSubject>
}
