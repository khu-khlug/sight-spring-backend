package com.sight.controllers.http

import com.sight.controllers.http.dto.GetDailyVisitCountResponse
import com.sight.controllers.http.dto.ListDoorLockMemberResponse
import com.sight.controllers.http.dto.toDoorLockMemberResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.DoorLockMemberService
import com.sight.service.RoomService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalDoorLockController(
    private val doorLockMemberService: DoorLockMemberService,
    private val roomService: RoomService,
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

    @Auth(roles = [UserRole.SYSTEM])
    @GetMapping("/internal/door-lock/daily-visit-count")
    fun getDailyVisitCount(
        @RequestParam room: Int,
    ): GetDailyVisitCountResponse {
        val count = roomService.getDailyVisitCount(room)
        return GetDailyVisitCountResponse(count = count)
    }
}
