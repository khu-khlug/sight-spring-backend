package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateGroupMatchingRequest
import com.sight.controllers.http.dto.CreateGroupMatchingResponse
import com.sight.controllers.http.dto.GetGroupMatchingAnswerResponse
import com.sight.controllers.http.dto.GetGroupMatchingGroupsResponse
import com.sight.controllers.http.dto.GroupMatchingGroupMemberResponse
import com.sight.controllers.http.dto.GroupMatchingResponse
import com.sight.controllers.http.dto.UpdateGroupMatchingClosedAtRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.domain.group.GroupCategory
import com.sight.service.GroupMatchingService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupMatchingController(
    private val groupMatchingService: GroupMatchingService,
) {
    @Auth([UserRole.MANAGER])
    @GetMapping("/group-matchings/{groupMatchingId}/groups")
    fun getGroups(
        @PathVariable groupMatchingId: String,
        @RequestParam(required = false) groupType: GroupCategory?,
    ): List<GetGroupMatchingGroupsResponse> {
        return groupMatchingService.getGroups(groupMatchingId, groupType).map { groupDto ->
            GetGroupMatchingGroupsResponse(
                id = groupDto.id,
                title = groupDto.title,
                members =
                    groupDto.members.map { memberDto ->
                        GroupMatchingGroupMemberResponse(
                            id = memberDto.id,
                            userId = memberDto.userId,
                            name = memberDto.name,
                            number = memberDto.number,
                        )
                    },
                createdAt = groupDto.createdAt,
            )
        }
    }

    @Auth(roles = [UserRole.USER, UserRole.MANAGER])
    @GetMapping("/group-matchings/{groupMatchingId}/answers/@me")
    fun getAnswer(
        @PathVariable groupMatchingId: String,
        requester: Requester,
    ): GetGroupMatchingAnswerResponse {
        val answerDto = groupMatchingService.getAnswer(groupMatchingId, requester.userId)
        return GetGroupMatchingAnswerResponse(
            id = answerDto.id,
            userId = answerDto.userId,
            groupType = answerDto.groupType,
            isPreferOnline = answerDto.isPreferOnline,
            groupMatchingId = answerDto.groupMatchingId,
            fields =
                answerDto.fields.map { field ->
                    GetGroupMatchingAnswerResponse.FieldResponse(
                        id = field.id,
                        name = field.name,
                    )
                },
            matchedGroups =
                answerDto.matchedGroups.map { matchedGroup ->
                    GetGroupMatchingAnswerResponse.MatchedGroupResponse(
                        id = matchedGroup.id,
                        groupId = matchedGroup.groupId,
                        createdAt = matchedGroup.createdAt,
                    )
                },
            groupMatchingSubjects =
                answerDto.groupMatchingSubjects.map { subject ->
                    GetGroupMatchingAnswerResponse.GroupMatchingSubjectResponse(
                        id = subject.id,
                        subject = subject.subject,
                    )
                },
            createdAt = answerDto.createdAt,
            updatedAt = answerDto.updatedAt,
        )
    }

    @Auth(roles = [UserRole.MANAGER])
    @PatchMapping("/group-matchings/{groupMatchingId}/closed-at")
    fun updateClosedAt(
        @PathVariable groupMatchingId: String,
        @Valid @RequestBody request: UpdateGroupMatchingClosedAtRequest,
    ): GroupMatchingResponse {
        val groupMatching = groupMatchingService.updateClosedAt(groupMatchingId, request.closedAt)

        return GroupMatchingResponse(
            groupMatchingId = groupMatching.id,
            year = groupMatching.year,
            semester = groupMatching.semester,
            closedAt = groupMatching.closedAt,
            createdAt = groupMatching.createdAt,
        )
    }

    @Auth([UserRole.MANAGER])
    @PostMapping("/group-matchings")
    @ResponseStatus(HttpStatus.CREATED)
    fun createGroupMatchingRequest(
        @Valid @RequestBody request: CreateGroupMatchingRequest,
    ): CreateGroupMatchingResponse {
        val saved =
            groupMatchingService.createGroupMatching(
                year = request.year,
                semester = request.semester,
                closedAt = request.closedAt,
            )

        return CreateGroupMatchingResponse(
            id = saved.id,
            year = saved.year,
            semester = saved.semester,
            createdAt = saved.createdAt,
            closedAt = saved.closedAt,
        )
    }
}
