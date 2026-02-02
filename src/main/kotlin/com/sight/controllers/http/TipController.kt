package com.sight.controllers.http

import com.sight.controllers.http.dto.GetCurrentTipResponse
import com.sight.service.TipService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TipController(private val tipService: TipService) {
    @GetMapping("/tip")
    fun getCurrentTip(): GetCurrentTipResponse {
        return GetCurrentTipResponse(content = tipService.getRandomTip())
    }
}
