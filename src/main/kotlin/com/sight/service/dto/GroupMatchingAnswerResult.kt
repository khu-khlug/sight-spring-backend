package com.sight.service.dto

import com.sight.domain.groupmatching.GroupMatchingAnswer

data class GroupMatchingAnswerResult(
    val answer: GroupMatchingAnswer,
    val options: List<OptionResult>,
)
