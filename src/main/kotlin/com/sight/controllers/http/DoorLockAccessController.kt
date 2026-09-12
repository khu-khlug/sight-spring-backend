package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateDoorLockAccessRequest
import com.sight.controllers.http.dto.CreateDoorLockAccessResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.DoorLockAccessService
import com.sight.service.DoorLockAlertService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class DoorLockAccessController(
    private val doorLockAccessService: DoorLockAccessService,
    private val doorLockAlertService: DoorLockAlertService,
) {
    @Auth([UserRole.SYSTEM])
    @PostMapping("/internal/door-lock/accesses")
    fun createDoorLockAccess(
        @Valid @RequestBody request: CreateDoorLockAccessRequest,
    ): CreateDoorLockAccessResponse {
        val result =
            doorLockAccessService.createDoorLockAccess(
                number = request.number!!,
                roomNumber = request.roomNumber!!,
            )
        return CreateDoorLockAccessResponse(name = result.name)
    }

    @Auth([UserRole.SYSTEM])
    @PostMapping("/internal/door-lock/alert-die")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun alertDoorLockDie() {
        doorLockAlertService.alertDie()
    }
}
