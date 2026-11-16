package com.sight.domain.group

// 그룹 내 검색어. 공백으로 나눈 검색어를 모두 포함해야 일치하는 AND 검색이며, 대소문자와 단어 순서는 따지지 않는다.
class GroupSearchKeywords private constructor(
    val terms: List<String>,
) {
    // 모든 검색어가 텍스트에 들어 있는지 확인한다.
    fun matches(text: String): Boolean = terms.all { text.contains(it, ignoreCase = true) }

    // 텍스트에서 가장 앞에 나오는 검색어의 위치와 길이를 반환한다. 검색어가 없으면 null이다.
    fun firstMatch(text: String): MatchPosition? =
        terms
            .mapNotNull { term ->
                val index = text.indexOf(term, ignoreCase = true)
                if (index < 0) null else MatchPosition(index, term.length)
            }.minByOrNull { it.index }

    data class MatchPosition(
        val index: Int,
        val length: Int,
    )

    companion object {
        const val MAX_LENGTH: Int = 20

        // 스페이스·탭·줄바꿈뿐 아니라 NBSP·전각 공백 같은 유니코드 공백 구분 문자도 공백으로 본다.
        private val WHITESPACES = Regex("[\\s\\p{Z}]+")

        // 검색 대상 텍스트의 연속된 공백을 하나로 정리하고 앞뒤 공백을 제거한다.
        fun normalizeText(text: String): String = text.replace(WHITESPACES, " ").trim()

        // 공백으로 나누고 빈 항목과 중복 검색어를 제거한다.
        fun parse(keyword: String): GroupSearchKeywords {
            require(keyword.codePointCount(0, keyword.length) <= MAX_LENGTH) {
                "검색어는 ${MAX_LENGTH}자 이하여야 합니다."
            }
            val terms =
                keyword
                    .split(WHITESPACES)
                    .filter { it.isNotEmpty() }
                    .distinctBy { it.lowercase() }
            require(terms.isNotEmpty()) { "검색어가 필요합니다." }
            return GroupSearchKeywords(terms)
        }
    }
}
