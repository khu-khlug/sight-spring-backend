package com.sight.controllers.http

import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalApiController {
    @Auth(roles = [UserRole.SYSTEM])
    @GetMapping("/internal/test")
    fun testInternalApi(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "message" to "Hello for internal API",
            ),
        )
    }
}
