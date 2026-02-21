package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateIdeaCloudRequest
import com.sight.controllers.http.dto.CreateIdeaCloudResponse
import com.sight.controllers.http.dto.IdeaCloudAuthorResponse
import com.sight.controllers.http.dto.IdeaCloudItemResponse
import com.sight.controllers.http.dto.ListIdeaCloudResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.IdeaCloudService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class IdeaCloudController(
    private val ideaCloudService: IdeaCloudService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/idea-clouds")
    fun listIdeaClouds(): ListIdeaCloudResponse {
        val ideas = ideaCloudService.listRandomPublicIdeas()

        val ideaClouds =
            ideas.map { idea ->
                IdeaCloudItemResponse(
                    id = idea.id,
                    content = idea.idea,
                    author =
                        IdeaCloudAuthorResponse(
                            id = idea.authorId,
                            realname = idea.authorName,
                        ),
                    createdAt = idea.createdAt,
                )
            }

        return ListIdeaCloudResponse(
            ideaClouds = ideaClouds,
            count = ideaClouds.size,
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/idea-clouds")
    @ResponseStatus(HttpStatus.CREATED)
    fun createIdeaCloud(
        requester: Requester,
        @Valid @RequestBody request: CreateIdeaCloudRequest,
    ): CreateIdeaCloudResponse {
        val ideaCloud = ideaCloudService.createIdea(requester.userId, request.content)

        return CreateIdeaCloudResponse(id = ideaCloud.id!!)
    }

    @Auth([UserRole.MANAGER])
    @DeleteMapping("/idea-clouds/{ideaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteIdeaCloud(
        @PathVariable ideaId: Long,
    ) {
        ideaCloudService.deleteIdea(ideaId)
    }
}
