package com.sight.service

import com.sight.repository.DocumentRepository
import com.sight.repository.projection.TalkWithAuthorProjection
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TalkService(
    private val documentRepository: DocumentRepository,
) {
    companion object {
        const val TALKS_BOARD_ID = 1L
        const val STATE_PUBLIC = "public"
    }

    @Transactional(readOnly = true)
    fun listTalks(
        offset: Int,
        limit: Int,
    ): TalkListResult {
        val talks =
            documentRepository.findTalksWithAuthor(
                TALKS_BOARD_ID,
                STATE_PUBLIC,
                offset,
                limit,
            )
        val total =
            documentRepository.countByBoardAndState(
                TALKS_BOARD_ID,
                STATE_PUBLIC,
            )
        return TalkListResult(count = total, talks = talks)
    }
}

data class TalkListResult(
    val count: Long,
    val talks: List<TalkWithAuthorProjection>,
)
