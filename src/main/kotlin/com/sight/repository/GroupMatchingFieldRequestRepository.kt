package com.sight.repository

import com.sight.domain.groupmatching.GroupMatchingFieldRequest
import org.springframework.data.jpa.repository.JpaRepository

interface GroupMatchingFieldRequestRepository : JpaRepository<GroupMatchingFieldRequest, String>
