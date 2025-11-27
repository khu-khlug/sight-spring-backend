package com.sight.controllers.http

import com.sight.controllers.http.dto.GetAnswersResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.GroupMatchingAnswerService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupMatchingAnswerController(
    private val answerService: GroupMatchingAnswerService,
) {
    @Auth([UserRole.MANAGER])
    @GetMapping("/group-matchings/{groupMatchingId}/answers")
    fun getAnswers(
        @PathVariable groupMatchingId: String,
        @RequestParam(required = false) groupType: String?,
        @RequestParam(required = false, defaultValue = "0") offset: Int,
        @RequestParam(required = false, defaultValue = "20") limit: Int,
    ): GetAnswersResponse {
        return answerService.getAllAnswers(groupMatchingId, groupType, offset, limit)
    }
}
