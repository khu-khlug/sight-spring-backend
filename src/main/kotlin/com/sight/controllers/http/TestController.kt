package com.sight.controllers.http

import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController {
    @GetMapping("/test/public")
    fun publicEndpoint(): String {
        return "This is a public endpoint"
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/test/protected")
    fun protectedEndpoint(requester: Requester): String {
        return "Hello user ${requester.userId} with role ${requester.role}"
    }

    @Auth([UserRole.MANAGER])
    @GetMapping("/test/admin")
    fun adminOnlyEndpoint(requester: Requester): String {
        return "Admin access for user ${requester.userId}"
    }
}
