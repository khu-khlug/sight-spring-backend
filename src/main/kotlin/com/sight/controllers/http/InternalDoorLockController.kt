package com.sight.controllers.http

import com.sight.controllers.http.dto.ListDoorLockMemberResponse
import com.sight.controllers.http.dto.toDoorLockMemberResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.DoorLockMemberService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalDoorLockController(
    private val doorLockMemberService: DoorLockMemberService,
) {
    @Auth(roles = [UserRole.SYSTEM])
    @GetMapping("/internal/door-lock/members")
    fun listDoorLockMembers(): ListDoorLockMemberResponse {
        val members = doorLockMemberService.listDoorLockMembers()
        return ListDoorLockMemberResponse(
            count = members.size.toLong(),
            members = members.map { it.toDoorLockMemberResponse() },
        )
    }
}
