package com.sight.controllers.http

import com.sight.controllers.http.dto.ListGroupMatchingOptionsResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.domain.groupmatching.GroupMatchingType
import com.sight.service.GroupMatchingOptionService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupMatchingOptionController(
    private val groupMatchingOptionService: GroupMatchingOptionService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/group-matchings/{groupMatchingId}/options")
    fun listOptions(
        @PathVariable groupMatchingId: String,
        @RequestParam type: GroupMatchingType,
    ): List<ListGroupMatchingOptionsResponse> {
        val options = groupMatchingOptionService.listOptions(groupMatchingId, type)
        return options.map { option ->
            ListGroupMatchingOptionsResponse(
                id = option.id,
                name = option.name,
            )
        }
    }
}
