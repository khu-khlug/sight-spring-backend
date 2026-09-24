package com.sight.core.book

interface BookInfoClient {
    // ISBN으로 도서 정보를 조회.
    // ISBN이 유효하지 않거나 정보가 없으면 null을 반환.
    // 네트워크 에러시 null을 반환.
    fun searchByIsbn(isbn: String): BookInfoItem?
}

// 외부 도서 정보. title을 제외한 필드는 외부 서비스가 값을 주지 않거나 유효하지 않은 값을 줄 수 있어 nullable이다.
// 빈 문자열(공백만 있는 값 포함)은 구현체가 null로 정규화해서 넘기므로, 소비하는 쪽은 "" 대신 null만 처리하면 된다.
data class BookInfoItem(
    // 비어 있지 않음. 정보 없으면 검색 결과 전체를 null로 반환
    val title: String,
    // 정보 없으면 null
    val author: String?,
    // 정보 없으면 null
    val publisher: String?,
    // 확인 불가하거나 MIN_PUBLISHED_YEAR..MAX_PUBLISHED_YEAR 범위 밖이면 null
    val publishedYear: Int?,
    // 표지 이미지 없으면 null
    val coverImageUrl: String?,
    // 내용 없으면 null
    val description: String?,
)

const val MIN_PUBLISHED_YEAR = 1900
const val MAX_PUBLISHED_YEAR = 3000

fun String.orNullIfBlank(): String? = takeIf { it.isNotBlank() }

fun Int.toPublishedYearOrNull(): Int? = takeIf { it in MIN_PUBLISHED_YEAR..MAX_PUBLISHED_YEAR }
