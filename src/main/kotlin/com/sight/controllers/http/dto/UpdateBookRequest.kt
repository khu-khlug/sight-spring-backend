package com.sight.controllers.http.dto

import com.sight.domain.book.BookCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class UpdateBookRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,
    @field:NotBlank(message = "저자는 필수입니다")
    val author: String,
    @field:NotBlank(message = "출판사는 필수입니다")
    val publisher: String,
    @field:NotNull(message = "발행연도는 필수입니다")
    @field:Positive(message = "발행연도는 양수여야 합니다")
    val publishedYear: Int?,
    val coverImageUrl: String,
    val description: String,
    @field:NotNull(message = "카테고리는 필수입니다")
    val category: BookCategory?,
)
