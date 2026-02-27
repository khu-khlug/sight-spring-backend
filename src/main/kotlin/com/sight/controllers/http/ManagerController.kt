package com.sight.controllers.http

import com.sight.controllers.http.dto.ListMemberRequest
import com.sight.controllers.http.dto.ListMemberResponse
import com.sight.controllers.http.dto.toResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.UserService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RestController

@RestController
class ManagerController(
    private val userService: UserService,
) {
    @Auth([UserRole.MANAGER])
    @GetMapping("/manager/users")
    fun listUsers(
        @Valid @ModelAttribute request: ListMemberRequest,
    ): ListMemberResponse {
        val (count, members) =
            userService.listMembers(
                email = request.email,
                phone = request.phone,
                name = request.name,
                number = request.number,
                college = request.college,
                grade = request.grade,
                studentStatus = request.studentStatus,
                limit = request.limit,
                offset = request.offset,
            )
        return ListMemberResponse(
            count = count,
            users = members.map { it.toResponse() },
        )
    }
}
