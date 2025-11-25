package com.sight.repository.projection

import java.time.LocalDateTime

interface GroupWithMemberProjection {
    val groupId: Long
    val groupTitle: String
    val groupCreatedAt: LocalDateTime
    val memberId: Long
    val memberName: String
    val memberRealName: String
    val memberNumber: Long
}
