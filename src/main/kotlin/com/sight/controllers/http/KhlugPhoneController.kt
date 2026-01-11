package com.sight.controllers.http

import com.sight.controllers.http.dto.ReportPhoneStatusRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.KhlugPhoneService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class KhlugPhoneController(
    private val khlugPhoneService: KhlugPhoneService,
) {
    @Auth(roles = [UserRole.SYSTEM])
    @PostMapping("/internal/khlug-phone/status")
    fun reportPhoneStatus(
        @Valid @RequestBody request: ReportPhoneStatusRequest,
    ): ResponseEntity<Void> {
        khlugPhoneService.reportPhoneStatus(request)
        return ResponseEntity.noContent().build()
    }
}
