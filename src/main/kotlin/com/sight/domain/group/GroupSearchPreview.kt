package com.sight.domain.group

// 기록 검색 결과의 일치 부분 미리보기. 검색에 사용한 정규화된 텍스트에서 일치 위치 주변만 잘라 만든다.
// 길이는 글자(코드 포인트) 단위이며 이모지 같은 서로게이트 쌍을 중간에서 자르지 않는다.
object GroupSearchPreview {
    const val BEFORE_LENGTH: Int = 30
    const val AFTER_LENGTH: Int = 70
    private const val ELLIPSIS = "…"

    fun create(match: GroupSearchKeywords.Match): String {
        val text = match.text
        val start = text.moveBackward(match.index, BEFORE_LENGTH)
        val end = text.moveForward(match.index + match.length, AFTER_LENGTH)

        val prefix = if (start > 0) ELLIPSIS else ""
        val suffix = if (end < text.length) ELLIPSIS else ""
        return prefix + text.substring(start, end) + suffix
    }

    // 지정한 위치에서 코드 포인트 `count`개만큼 앞으로 이동한 위치. 텍스트 처음을 넘지 않는다.
    private fun String.moveBackward(
        index: Int,
        count: Int,
    ): Int {
        var position = index
        var remaining = count
        while (position > 0 && remaining > 0) {
            position -= Character.charCount(codePointBefore(position))
            remaining--
        }
        return position
    }

    // 지정한 위치에서 코드 포인트 `count`개만큼 뒤로 이동한 위치. 텍스트 끝을 넘지 않는다.
    private fun String.moveForward(
        index: Int,
        count: Int,
    ): Int {
        var position = index
        var remaining = count
        while (position < length && remaining > 0) {
            position += Character.charCount(codePointAt(position))
            remaining--
        }
        return position
    }
}
