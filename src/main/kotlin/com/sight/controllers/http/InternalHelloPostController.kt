package com.sight.controllers.http

import com.sight.controllers.http.dto.ListHelloPostsResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.HelloPostService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class InternalHelloPostController(
    private val helloPostService: HelloPostService,
) {
    @Auth(roles = [UserRole.SYSTEM])
    @GetMapping("/internal/hello-posts")
    fun listHelloPosts(
        @RequestParam(defaultValue = "10")
        @Min(1)
        @Max(10)
        limit: Int,
    ): ListHelloPostsResponse {
        return ListHelloPostsResponse.from(helloPostService.listHelloPosts(limit))
    }
}
