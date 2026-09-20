package com.sight.domain.book

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "book_info")
class BookInfo(
    id: String,
    isbn: String,
    title: String,
    author: String,
    publisher: String,
    publishedYear: Int,
    coverImageUrl: String,
    description: String,
    category: BookCategory,
) {
    @Id
    @Column(name = "id", nullable = false, length = 26)
    val id: String = id

    @Column(name = "isbn", nullable = false, unique = true, length = 13)
    val isbn: String = isbn

    @Column(name = "title", nullable = false, length = 255)
    var title: String = title
        private set

    @Column(name = "author", nullable = false, length = 255)
    var author: String = author
        private set

    @Column(name = "publisher", nullable = false, length = 255)
    var publisher: String = publisher
        private set

    @Column(name = "published_year", nullable = false)
    var publishedYear: Int = publishedYear
        private set

    @Column(name = "cover_image_url", nullable = false, length = 500)
    var coverImageUrl: String = coverImageUrl
        private set

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    var description: String = description
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50, columnDefinition = "VARCHAR(50) DEFAULT 'OTHER'")
    var category: BookCategory = category
        private set

    fun update(
        title: String,
        author: String,
        publisher: String,
        publishedYear: Int,
        coverImageUrl: String,
        description: String,
        category: BookCategory,
    ) {
        require(title.isNotBlank()) { "제목은 비어있을 수 없습니다" }
        require(author.isNotBlank()) { "저자는 비어있을 수 없습니다" }
        require(publisher.isNotBlank()) { "출판사는 비어있을 수 없습니다" }
        require(publishedYear > 0) { "발행연도는 양수여야 합니다" }

        this.title = title
        this.author = author
        this.publisher = publisher
        this.publishedYear = publishedYear
        this.coverImageUrl = coverImageUrl
        this.description = description
        this.category = category
    }
}
