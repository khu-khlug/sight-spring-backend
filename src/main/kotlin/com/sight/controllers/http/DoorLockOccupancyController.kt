package com.sight.controllers.http

import com.sight.controllers.http.dto.ListDoorLockOccupantResponse
import com.sight.controllers.http.dto.ListDoorLockOccupantsResponse
import com.sight.controllers.http.dto.UpdateDoorLockOccupantsRequest
import com.sight.controllers.http.dto.UpdateDoorLockOccupantsResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.core.exception.BadRequestException
import com.sight.service.DoorLockOccupancyService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class DoorLockOccupancyController(
    private val doorLockOccupancyService: DoorLockOccupancyService,
) {
    @Auth([UserRole.SYSTEM])
    @PutMapping("/occupants")
    @ResponseStatus(HttpStatus.OK)
    fun updateDoorLockOccupants(
        @Valid @RequestBody request: UpdateDoorLockOccupantsRequest,
    ): UpdateDoorLockOccupantsResponse {
        val numbers =
            request.occupants!!.map { occupant ->
                if (occupant == null || !STUDENT_NUMBER_PATTERN.matches(occupant)) {
                    throw BadRequestException("학번은 10자리 숫자여야 합니다")
                }
                occupant.toLong()
            }
        doorLockOccupancyService.updateOccupants(
            numbers = numbers,
        )
        return UpdateDoorLockOccupantsResponse(status = "OK")
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/occupants")
    @ResponseStatus(HttpStatus.OK)
    fun listDoorLockOccupants(): ListDoorLockOccupantsResponse {
        val result = doorLockOccupancyService.listOccupants()
        return ListDoorLockOccupantsResponse(
            occupants = result.occupantNames.map(::ListDoorLockOccupantResponse),
            lastSyncedAt = result.lastSyncedAt,
        )
    }

    companion object {
        private val STUDENT_NUMBER_PATTERN = Regex("^[0-9]{10}$")
    }
}
