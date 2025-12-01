package com.sight.service.dto

import com.sight.domain.groupmatching.GroupMatchingAnswer

data class GroupMatchingAnswerResult(
    val answer: GroupMatchingAnswer,
    val fieldIds: List<String>,
    val subjectIds: List<String>,
)
