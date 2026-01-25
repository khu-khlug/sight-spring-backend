package com.sight.controllers.http

import com.sight.controllers.http.dto.ListTalksResponse
import com.sight.controllers.http.dto.TalkAuthorResponse
import com.sight.controllers.http.dto.TalkResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.TalkService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class TalkController(
    private val talkService: TalkService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/talks")
    fun listTalks(
        @RequestParam(defaultValue = "0") @Min(0, message = "offset은 0 이상이어야 합니다") offset: Int,
        @RequestParam(defaultValue = "10") @Min(1, message = "limit은 최소 1입니다") @Max(100, message = "limit은 최대 100입니다") limit: Int,
    ): ListTalksResponse {
        val result = talkService.listTalks(offset, limit)
        return ListTalksResponse(
            count = result.count,
            talks =
                result.talks.map { talk ->
                    TalkResponse(
                        id = talk.id,
                        title = talk.title,
                        author =
                            TalkAuthorResponse(
                                id = talk.authorId,
                                realname = talk.authorRealname,
                            ),
                        createdAt = talk.createdAt,
                    )
                },
        )
    }
}
