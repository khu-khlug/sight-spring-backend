package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.domain.ideacloud.IdeaCloud
import com.sight.domain.ideacloud.IdeaCloudState
import com.sight.repository.IdeaCloudRepository
import com.sight.repository.projection.IdeaCloudWithAuthorProjection
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class IdeaCloudService(
    private val ideaCloudRepository: IdeaCloudRepository,
    private val pointService: PointService,
) {
    @Transactional(readOnly = true)
    fun listRandomPublicIdeas(): List<IdeaCloudWithAuthorProjection> {
        return ideaCloudRepository.findRandomPublicIdeasWithAuthor()
    }

    @Transactional
    fun createIdea(
        authorId: Long,
        idea: String,
    ): IdeaCloud {
        val ideaCloud =
            IdeaCloud(
                idea = idea,
                author = authorId,
                state = IdeaCloudState.PUBLIC,
            )
        val savedIdeaCloud = ideaCloudRepository.save(ideaCloud)

        pointService.givePoint(
            targetUserId = authorId,
            point = 5,
            message = "아이디어 클라우드에 아이디어를 추가했습니다.",
        )

        return savedIdeaCloud
    }

    @Transactional
    fun deleteIdea(ideaId: Long) {
        val ideaCloud =
            ideaCloudRepository.findById(ideaId).orElseThrow {
                NotFoundException("아이디어를 찾을 수 없습니다")
            }
        ideaCloudRepository.delete(ideaCloud)

        pointService.givePoint(
            targetUserId = ideaCloud.author,
            point = -5,
            message = "아이디어 클라우드에서 아이디어가 삭제되었습니다.",
        )
    }
}
