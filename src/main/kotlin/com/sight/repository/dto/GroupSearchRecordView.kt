package com.sight.repository.dto

import com.sight.domain.group.GroupRecordType

// 기록 본문 검색 대상. 본문은 레거시 HTML 또는 Tiptap JSON 원문이다.
data class GroupSearchRecordView(
    val recordId: Long,
    val cardId: Long,
    val type: GroupRecordType,
    val content: String,
)
