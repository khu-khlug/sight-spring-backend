package com.sight.controllers.http

import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class UserManageController(
    private val userService: UserService,
) {
    @Auth([UserRole.MANAGER])
    @PutMapping("/manager/users/{userId}/manager")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun appointManager(
        requester: Requester,
        @PathVariable userId: Long,
    ) {
        userService.appointManager(
            requesterUserId = requester.userId,
            targetUserId = userId,
        )
    }

    @Auth([UserRole.MANAGER])
    @DeleteMapping("/manager/users/{userId}/manager")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun stepdownManager(
        requester: Requester,
        @PathVariable userId: Long,
    ) {
        userService.stepdownManager(
            requesterUserId = requester.userId,
            targetUserId = userId,
        )
    }

    @Auth([UserRole.MANAGER])
    @PostMapping("/manager/users/{userId}/graduation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun graduateUser(
        @PathVariable userId: Long,
    ) {
        userService.graduateMember(userId)
    }

    @Auth([UserRole.MANAGER])
    @DeleteMapping("/manager/users/{userId}/graduation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun ungraduateUser(
        @PathVariable userId: Long,
    ) {
        userService.ungraduateMember(userId)
    }
}
