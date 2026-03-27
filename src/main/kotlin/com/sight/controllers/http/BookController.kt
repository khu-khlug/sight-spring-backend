package com.sight.controllers.http

import com.sight.controllers.http.dto.GetMyBorrowingsResponse
import com.sight.controllers.http.dto.MyBorrowingItemResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.BookService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class BookController(
    private val bookService: BookService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/book/borrowings/@me")
    fun getMyBorrowings(requester: Requester): GetMyBorrowingsResponse {
        val results = bookService.getMyBorrowings(requester.userId)
        return GetMyBorrowingsResponse(
            currentBorrowings =
                results.map { result ->
                    MyBorrowingItemResponse(
                        bookId = result.bookId,
                        itemId = result.itemId,
                        title = result.title,
                        borrowedAt = result.borrowedAt,
                    )
                },
        )
    }
}
