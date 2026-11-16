package com.sight.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.domain.group.GroupRecordType
import com.sight.domain.group.GroupSearchKeywords
import org.jsoup.Jsoup
import org.springframework.stereotype.Component

// 저장된 기록 본문(레거시 HTML, Tiptap JSON)에서 화면에 표시되는 텍스트만 추출한다.
// 저장된 원문은 바꾸지 않으며, 검색과 미리보기에 같은 추출 결과를 사용한다.
@Component
class GroupRecordTextExtractor(
    private val objectMapper: ObjectMapper,
) {
    fun extract(
        type: GroupRecordType,
        content: String,
    ): String =
        when (type) {
            GroupRecordType.LEGACY -> extractFromHtml(content)
            GroupRecordType.TIPTAP -> extractFromTiptap(content)
        }

    private fun extractFromHtml(html: String): String {
        val document = Jsoup.parse(html)
        // 화면에 표시하지 않는 요소는 제외한다.
        document.select(HIDDEN_ELEMENT_SELECTOR).remove()
        // body의 텍스트는 엔티티를 해제하고, 블록 요소·줄바꿈 경계를 공백으로 구분하며, 연속 공백을 하나로 정리한다.
        return GroupSearchKeywords.normalizeText(document.body().text())
    }

    private fun extractFromTiptap(json: String): String {
        val root =
            try {
                objectMapper.readTree(json)
            } catch (e: Exception) {
                // 깨진 본문 하나 때문에 검색 전체가 실패하지 않도록 검색 대상에서 제외한다.
                return ""
            }
        val builder = StringBuilder()
        appendNode(root, builder)
        return GroupSearchKeywords.normalizeText(builder.toString())
    }

    private fun appendNode(
        node: JsonNode,
        builder: StringBuilder,
    ) {
        if (!node.isObject) return
        val type = node.path("type").asText("")

        if (type == "text") {
            // 인라인 서식(mark)으로 나뉜 텍스트는 그대로 이어 붙인다.
            builder.append(node.path("text").asText(""))
            return
        }

        // 문단·목록 항목·표 셀·줄바꿈 등 블록 노드의 경계는 공백으로 구분한다. 문장 안에 들어가는 인라인 노드는 이어 붙인다.
        val boundary = if (type in INLINE_NODE_TYPES) "" else " "
        builder.append(boundary)
        if (type != "hardBreak") {
            // 본문 텍스트를 속성에 저장하는 노드는 해당 속성을 읽는다. 파일명·대체 텍스트·URL 같은 메타데이터는 읽지 않는다.
            TEXT_ATTRIBUTES[type]?.let { attribute ->
                builder.append(node.path("attrs").path(attribute).asText(""))
            }
            node.path("content").forEach { child -> appendNode(child, builder) }
        }
        builder.append(boundary)
    }

    companion object {
        private const val HIDDEN_ELEMENT_SELECTOR =
            "script, style, noscript, template, head, [hidden], input[type=hidden], " +
                "[style*=display:none], [style*=display: none], [style*=visibility:hidden], [style*=visibility: hidden]"

        // 문장 안에 들어가는 인라인 노드. 프런트(sight-frontend `BlockContent/extensions.ts`)에서 `text`·`hardBreak` 외 인라인 노드는 인라인 수식뿐이다.
        private val INLINE_NODE_TYPES = setOf("inlineMath")

        // 본문 텍스트가 `text` 노드가 아니라 속성에 저장되는 노드. 수식은 LaTeX 원문이 본문이다.
        // 파일 첨부·이미지·오디오·비디오·유튜브는 `src`·`name`·`alt`·`title` 메타데이터만 가지므로 읽지 않고, 목차는 저장되는 본문이 없다.
        private val TEXT_ATTRIBUTES =
            mapOf(
                "inlineMath" to "latex",
                "blockMath" to "latex",
            )
    }
}
