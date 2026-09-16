package com.sight.core.book

interface BookInfoClient {
    fun searchByIsbn(isbn: String): BookInfoItem?
}

data class BookInfoItem(
    // 빈 문자열이면 안 됨. 정보 없으면 검색 결과 전체를 null로 반환
    val title: String,
    // 정보 없으면 빈 문자열
    val author: String,
    // 정보 없으면 빈 문자열
    val publisher: String,
    // 확인 불가하면 0
    val publishedYear: Int,
    // 표지 이미지 없으면 빈 문자열
    val coverImageUrl: String,
    // 내용 없으면 빈 문자열
    val description: String,
)
