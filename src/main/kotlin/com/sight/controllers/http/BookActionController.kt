package com.sight.controllers.http

import com.sight.controllers.http.dto.RegisterBookResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.BookActionService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class BookActionController(
    private val bookActionService: BookActionService,
) {
    @Auth([UserRole.MANAGER])
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/book/register")
    fun registerBook(
        @RequestParam isbn: String,
        request: HttpServletRequest,
    ): RegisterBookResponse {
        val clientIp = request.remoteAddr
        val bookId = bookActionService.registerBook(isbn, clientIp)
        return RegisterBookResponse(bookId = bookId)
    }
}
