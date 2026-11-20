package com.sight.repository.dto

import com.sight.domain.group.GroupRecordType

// 검색 결과 카드와 그 카드가 속한 리스트 정보.
data class GroupSearchCardView(
    val listId: Long,
    val listTitle: String,
    val cardId: Long,
    val cardTitle: String,
    val authorName: String,
)

// 기록 본문 검색 대상. 본문은 레거시 HTML 또는 Tiptap JSON 원문이다.
data class GroupSearchRecordView(
    val recordId: Long,
    val cardId: Long,
    val type: GroupRecordType,
    val content: String,
)
