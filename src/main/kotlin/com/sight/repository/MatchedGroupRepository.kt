package com.sight.repository

import com.sight.domain.groupmatching.MatchedGroup
import org.springframework.data.jpa.repository.JpaRepository

interface MatchedGroupRepository : JpaRepository<MatchedGroup, String>
