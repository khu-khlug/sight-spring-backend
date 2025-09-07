package com.sight.controllers.http

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class PingController {
    @GetMapping("/ping")
    fun ping(): String {
        return "pong"
    }
}
