package com.sight.controllers.http

import com.sight.controllers.http.dto.GetBookStatsResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.BookService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class BookController(
    private val bookService: BookService,
) {
    @Auth([UserRole.MANAGER])
    @GetMapping("/book/stats")
    fun getStats(): GetBookStatsResponse {
        val result = bookService.getStats()
        return GetBookStatsResponse(
            totalBookCount = result.totalBookCount,
            totalItemCount = result.totalItemCount,
            currentBorrowingCount = result.currentBorrowingCount,
        )
    }
}
