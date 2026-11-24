package com.sight.repository.dto

import com.sight.domain.group.GroupRecordType
import java.time.Instant

// 회원이 보관한 기록과 그 기록의 현재 소속 그룹·카드, 작성자 실명.
data class SavedRecordView(
    val recordId: Long,
    val type: GroupRecordType,
    val content: String,
    val groupId: Long,
    val groupName: String,
    val cardId: Long,
    val cardTitle: String,
    val isDeleted: Boolean,
    val isCardDeleted: Boolean,
    val authorName: String,
    val createdAt: Instant,
    val savedAt: Instant,
)
