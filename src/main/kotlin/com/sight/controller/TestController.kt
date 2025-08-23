package com.sight.controller

import com.sight.common.AuthenticationHelper
import com.sight.config.security.Auth
import com.sight.domain.auth.UserRole
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
class TestController {

    @GetMapping("/public")
    fun publicEndpoint(): String {
        return "This is a public endpoint"
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/protected")
    fun protectedEndpoint(): String {
        val requester = AuthenticationHelper.requireCurrentRequester()
        return "Hello user ${requester.userId} with role ${requester.role}"
    }

    @Auth([UserRole.MANAGER])
    @GetMapping("/admin")
    fun adminOnlyEndpoint(): String {
        val requester = AuthenticationHelper.requireCurrentRequester()
        return "Admin access for user ${requester.userId}"
    }
}