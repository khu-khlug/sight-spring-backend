package com.sight.domain.group

// 기록 검색 결과의 일치 부분 미리보기. 검색에 사용한 표시 텍스트에서 일치 위치 주변만 잘라 만든다.
object GroupSearchPreview {
    const val BEFORE_LENGTH: Int = 30
    const val AFTER_LENGTH: Int = 70
    private const val ELLIPSIS = "…"

    fun create(
        text: String,
        keywords: GroupSearchKeywords,
    ): String {
        val match = keywords.firstMatch(text) ?: return text.take(BEFORE_LENGTH + AFTER_LENGTH)

        var start = (match.index - BEFORE_LENGTH).coerceAtLeast(0)
        var end = (match.index + match.length + AFTER_LENGTH).coerceAtMost(text.length)
        // 서로게이트 쌍(이모지 등)을 중간에서 자르지 않는다.
        if (start > 0 && text[start].isLowSurrogate()) start++
        if (end < text.length && text[end].isLowSurrogate()) end--

        val prefix = if (start > 0) ELLIPSIS else ""
        val suffix = if (end < text.length) ELLIPSIS else ""
        return prefix + text.substring(start, end) + suffix
    }
}
