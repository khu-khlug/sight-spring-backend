package com.sight.domain.book

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "book_info")
data class BookInfo(
    @Id
    @Column(name = "id", nullable = false, length = 26)
    val id: String,

    @Column(name = "isbn", nullable = false, unique = true, length = 13)
    val isbn: String,

    @Column(name = "title", nullable = false, length = 255)
    val title: String,

    @Column(name = "author", nullable = false, length = 255)
    val author: String,

    @Column(name = "publisher", nullable = false, length = 255)
    val publisher: String,

    @Column(name = "published_year", nullable = false)
    val publishedYear: Int,

    @Column(name = "cover_image_url", nullable = false, length = 500)
    val coverImageUrl: String,

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    val description: String,
)
