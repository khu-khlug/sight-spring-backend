package com.sight.controllers.http

import com.sight.controllers.http.dto.GetBookBorrowerInfoResponse
import com.sight.controllers.http.dto.GetBookItemResponse
import com.sight.controllers.http.dto.GetBookPreviewResponse
import com.sight.controllers.http.dto.GetBookResponse
import com.sight.controllers.http.dto.GetBookStatsResponse
import com.sight.controllers.http.dto.ListBookResponse
import com.sight.controllers.http.dto.ListBooksResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.BookService
import com.sight.service.dto.GetBookResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
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

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/book", params = ["!isbn"])
    fun listBooks(): ListBooksResponse {
        val results = bookService.listBooks()
        return ListBooksResponse(
            bookList =
                results.map { result ->
                    ListBookResponse(
                        bookId = result.bookId,
                        title = result.title,
                        coverImageUrl = result.coverImageUrl,
                        author = result.author,
                        publisher = result.publisher,
                        publishedYear = result.publishedYear,
                        totalCount = result.totalCount,
                        availableCount = result.availableCount,
                    )
                },
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/book/{bookId}")
    fun getBook(
        @PathVariable bookId: String,
    ): GetBookResponse {
        val result = bookService.getBook(bookId)
        return result.toResponse()
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/book", params = ["isbn"])
    fun getBookByIsbn(
        @RequestParam isbn: String,
    ): GetBookResponse {
        val result = bookService.getBookByIsbn(isbn)
        return result.toResponse()
    }

    @Auth([UserRole.MANAGER])
    @GetMapping("/book/preview")
    fun previewBook(
        @RequestParam isbn: String,
    ): GetBookPreviewResponse {
        val result = bookService.previewBook(isbn)
        return GetBookPreviewResponse(
            title = result.title,
            author = result.author,
            coverImageUrl = result.coverImageUrl,
            publisher = result.publisher,
            publishedYear = result.publishedYear,
            description = result.description,
        )
    }

    private fun GetBookResult.toResponse() =
        GetBookResponse(
            bookId = bookId,
            title = title,
            coverImageUrl = coverImageUrl,
            author = author,
            publisher = publisher,
            publishedYear = publishedYear,
            totalCount = totalCount,
            availableCount = availableCount,
            isbn = isbn,
            description = description,
            itemList =
                itemList.map { item ->
                    GetBookItemResponse(
                        itemId = item.itemId,
                        registeredAt = item.registeredAt,
                        borrowerInfo =
                            item.borrowerInfo?.let {
                                GetBookBorrowerInfoResponse(
                                    borrowerUserId = it.borrowerUserId,
                                    borrowerUserName = it.borrowerUserName,
                                    borrowedAt = it.borrowedAt,
                                )
                            },
                    )
                },
        )
}
