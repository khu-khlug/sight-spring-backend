package com.sight.controllers.http

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalApiController {
    @GetMapping("/internal/test")
    fun testInternalApi(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "message" to "Hello for internal API",
            ),
        )
    }
}
