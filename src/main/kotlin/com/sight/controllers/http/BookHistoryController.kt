package com.sight.controllers.http

import com.sight.controllers.http.dto.BorrowHistoryRecordResponse
import com.sight.controllers.http.dto.CurrentBorrowingRecordResponse
import com.sight.controllers.http.dto.GetBorrowHistoryResponse
import com.sight.controllers.http.dto.GetCurrentBorrowingsResponse
import com.sight.controllers.http.dto.GetMyBorrowingsResponse
import com.sight.controllers.http.dto.MyBorrowingItemResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.BookHistoryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class BookHistoryController(
    private val bookHistoryService: BookHistoryService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/book/borrowings/@me")
    fun getMyBorrowings(requester: Requester): GetMyBorrowingsResponse {
        val results = bookHistoryService.getMyBorrowings(requester.userId)
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

    @Auth([UserRole.MANAGER])
    @GetMapping("/book/borrow-history")
    fun getBorrowHistory(): GetBorrowHistoryResponse {
        val results = bookHistoryService.getBorrowHistory()
        return GetBorrowHistoryResponse(
            records =
                results.map { result ->
                    BorrowHistoryRecordResponse(
                        recordId = result.recordId,
                        itemId = result.itemId,
                        bookId = result.bookId,
                        title = result.title,
                        borrowerUserId = result.borrowerUserId,
                        borrowerUserName = result.borrowerUserName,
                        borrowedAt = result.borrowedAt,
                        returnedAt = result.returnedAt,
                    )
                },
        )
    }

    @Auth([UserRole.MANAGER])
    @GetMapping("/book/borrowings")
    fun getCurrentBorrowings(): GetCurrentBorrowingsResponse {
        val results = bookHistoryService.getCurrentBorrowings()
        return GetCurrentBorrowingsResponse(
            records =
                results.map { result ->
                    CurrentBorrowingRecordResponse(
                        recordId = result.recordId,
                        itemId = result.itemId,
                        bookId = result.bookId,
                        title = result.title,
                        borrowerUserId = result.borrowerUserId,
                        borrowerUserName = result.borrowerUserName,
                        borrowedAt = result.borrowedAt,
                    )
                },
        )
    }
}
