package com.sight.controllers.http

import com.sight.controllers.http.dto.RegisterBookResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.BookActionService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
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
        val bookId = bookActionService.registerBook(isbn, request.clientIp)
        return RegisterBookResponse(bookId = bookId)
    }

    @Auth([UserRole.MANAGER])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/book/{bookId}")
    fun deleteBook(
        @PathVariable bookId: String,
    ) {
        bookActionService.deleteBook(bookId)
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/book/{bookId}/return")
    fun returnBook(
        @PathVariable bookId: String,
        requester: Requester,
        request: HttpServletRequest,
    ) {
        bookActionService.returnBook(bookId, requester.userId, request.clientIp)
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/book/{bookId}/borrow")
    fun borrowBook(
        @PathVariable bookId: String,
        requester: Requester,
        request: HttpServletRequest,
    ) {
        bookActionService.borrowBook(bookId, requester.userId, request.clientIp)
    }

    private val HttpServletRequest.clientIp: String
        get() = getHeader("X-Forwarded-For")?.split(",")?.first()?.trim() ?: remoteAddr
}
