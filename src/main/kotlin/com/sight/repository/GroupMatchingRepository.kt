package com.sight.repository

import com.sight.domain.groupmatching.GroupMatching
import org.springframework.data.jpa.repository.JpaRepository

interface GroupMatchingRepository : JpaRepository<GroupMatching, String>
