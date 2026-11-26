package com.sight.domain.group

import java.text.Normalizer

// 그룹 내 검색어. 공백으로 나눈 검색어를 모두 포함해야 일치하는 AND 검색이며 단어 순서는 따지지 않는다.
// 비교 대상 텍스트와 검색어는 모두 NFC로 정규화해 같은 글자를 같은 형태로 맞춘 뒤 비교한다.
// 무시하는 차이는 대소문자 하나뿐이다. `A`와 `a`는 같은 글자로 본다.
// 악센트(é와 e)와 전각·반각(Ａ와 A)은 무시하지 않고 서로 다른 글자로 구분한다.
class GroupSearchKeywords private constructor(
    val terms: List<String>,
) {
    // 텍스트를 정규화해 모든 검색어가 들어 있는지 확인한다. 일치하지 않으면 null이다.
    fun find(text: String): Match? {
        val normalized = normalizeText(text)
        val firstIndex = terms.map { normalized.indexOf(it, ignoreCase = true) }
        if (firstIndex.any { it < 0 }) return null
        val first = firstIndex.indices.minBy { firstIndex[it] }
        return Match(normalized, firstIndex[first], terms[first].length)
    }

    // 일치한 텍스트와 가장 앞에 나오는 검색어의 위치. 위치는 정규화된 텍스트 기준이다.
    data class Match(
        val text: String,
        val index: Int,
        val length: Int,
    )

    companion object {
        // 검색어 최대 길이. NFC로 정규화한 뒤의 글자(코드 포인트) 수 기준이다.
        const val MAX_LENGTH: Int = 20

        // 스페이스·탭·줄바꿈뿐 아니라 NBSP·전각 공백 같은 유니코드 공백 구분 문자도 공백으로 본다.
        private val WHITESPACES = Regex("[\\s\\p{Z}]+")

        // 검색 대상 텍스트를 NFC로 정규화하고 연속된 공백을 하나로 정리하며 앞뒤 공백을 제거한다.
        fun normalizeText(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFC).replace(WHITESPACES, " ").trim()

        // NFC로 정규화한 뒤 공백으로 나누고 빈 항목과 중복 검색어를 제거한다. 길이는 정규화한 글자(코드 포인트) 수로 센다.
        fun parse(keyword: String): GroupSearchKeywords {
            val normalized = Normalizer.normalize(keyword, Normalizer.Form.NFC)
            require(normalized.codePointCount(0, normalized.length) <= MAX_LENGTH) {
                "검색어는 ${MAX_LENGTH}자 이하여야 합니다."
            }
            val terms =
                normalized
                    .split(WHITESPACES)
                    .filter { it.isNotEmpty() }
                    .distinctBy { it.lowercase() }
            require(terms.isNotEmpty()) { "검색어가 필요합니다." }
            return GroupSearchKeywords(terms)
        }
    }
}
