package com.sight.service.dto

import java.time.Instant

data class ListProvisionalGroupsResult(
    val provisionalGroups: List<ProvisionalGroupDetail>,
)

data class ProvisionalGroupDetail(
    val id: String,
    val name: String,
    val groupMatchingId: String,
    val answerCount: Long,
    val createdAt: Instant,
)
