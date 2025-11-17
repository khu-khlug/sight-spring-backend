package com.sight.repository

import com.sight.domain.groupmatching.GroupMatchingAnswer
import org.springframework.data.jpa.repository.JpaRepository

interface GroupMatchingAnswerRepository : JpaRepository<GroupMatchingAnswer, String>
