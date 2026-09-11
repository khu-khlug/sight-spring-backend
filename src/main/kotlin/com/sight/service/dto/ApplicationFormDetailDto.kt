package com.sight.service.dto

import com.sight.domain.application.ApplicationComment
import com.sight.domain.application.ApplicationContent
import com.sight.domain.application.ApplicationForm
import com.sight.domain.application.InterviewAvailableTime

data class ApplicationFormDetailDto(
    val form: ApplicationForm,
    val contents: List<ApplicationContent>,
    val times: List<InterviewAvailableTime>,
    val comments: List<ApplicationComment>,
)
