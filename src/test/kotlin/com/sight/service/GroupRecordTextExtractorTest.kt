package com.sight.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.domain.group.GroupRecordType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.text.Normalizer

class GroupRecordTextExtractorTest {
    private val extractor = GroupRecordTextExtractor(ObjectMapper())

    private fun html(content: String) = extractor.extract(GroupRecordType.LEGACY, content)

    private fun tiptap(content: String) = extractor.extract(GroupRecordType.TIPTAP, content)

    @Test
    fun `HTML 인라인 태그로 나뉜 텍스트는 이어 붙인다`() {
        assertEquals("안녕하세요", html("<b>안녕</b>하세요"))
    }

    @Test
    fun `HTML 엔티티는 해제한다`() {
        assertEquals("A & B", html("<p>A &amp; B</p>"))
    }

    @Test
    fun `HTML 문단·목록·표 셀·줄바꿈 경계는 공백으로 구분한다`() {
        assertEquals("가 나 다 라 마 바", html("<p>가</p><ul><li>나</li></ul><table><tr><td>다</td><td>라</td></tr></table>마<br>바"))
    }

    @Test
    fun `HTML 스크립트·스타일·숨김 요소와 속성은 제외한다`() {
        val text =
            html(
                """<style>.a{}</style><script>var secret = 1</script><p hidden>숨김</p>
                |<div style="display: none">안보임</div><img alt="대체텍스트" src="x.png"><a href="http://link.example">링크글</a>
                """.trimMargin(),
            )

        assertEquals("링크글", text)
    }

    @Test
    fun `Tiptap 인라인 서식으로 나뉜 텍스트는 이어 붙인다`() {
        val json =
            """{"type":"doc","content":[{"type":"paragraph","content":[
                |{"type":"text","text":"안녕","marks":[{"type":"bold"}]},{"type":"text","text":"하세요"}]}]}
            """.trimMargin()

        assertEquals("안녕하세요", tiptap(json))
    }

    @Test
    fun `Tiptap JSON 이스케이프는 파싱한 뒤의 실제 문자로 읽는다`() {
        val json = """{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"\"인용\" \\ 가"}]}]}"""

        assertEquals("\"인용\" \\ 가", tiptap(json))
    }

    @Test
    fun `Tiptap 문단·목록 항목·표 셀·줄바꿈 경계는 공백으로 구분한다`() {
        val json =
            """{"type":"doc","content":[
                |{"type":"paragraph","content":[{"type":"text","text":"가"},{"type":"hardBreak"},{"type":"text","text":"나"}]},
                |{"type":"bulletList","content":[{"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"다"}]}]}]},
                |{"type":"table","content":[{"type":"tableRow","content":[
                |{"type":"tableCell","content":[{"type":"paragraph","content":[{"type":"text","text":"라"}]}]},
                |{"type":"tableCell","content":[{"type":"paragraph","content":[{"type":"text","text":"마"}]}]}]}]}]}
            """.trimMargin()

        assertEquals("가 나 다 라 마", tiptap(json))
    }

    @Test
    fun `Tiptap 노드 종류·속성명·파일명·대체 텍스트·URL은 읽지 않는다`() {
        val json =
            """{"type":"doc","content":[
                |{"type":"paragraph","content":[{"type":"text","text":"본문"}]},
                |{"type":"fileAttachment","attrs":{"name":"비밀.pdf","src":"https://files.example/a"}},
                |{"type":"image","attrs":{"alt":"대체","src":"https://img.example/b.png","title":"미디어제목"}}]}
            """.trimMargin()

        val text = tiptap(json)

        assertEquals("본문", text)
        assertFalse(text.contains("paragraph"))
    }

    @Test
    fun `Tiptap 일반 본문에 쓴 파일명과 URL은 읽는다`() {
        val json = """{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"https://example.com/문서.pdf"}]}]}"""

        assertTrue(tiptap(json).contains("문서.pdf"))
    }

    @Test
    fun `Tiptap 코드·접기 블록 안의 텍스트와 수식 원문을 읽는다`() {
        val json =
            """{"type":"doc","content":[
                |{"type":"codeBlock","content":[{"type":"text","text":"val a = 1"}]},
                |{"type":"details","content":[{"type":"detailsSummary","content":[{"type":"text","text":"요약"}]},
                |{"type":"detailsContent","content":[{"type":"paragraph","content":[{"type":"text","text":"내용"}]}]}]},
                |{"type":"blockMath","attrs":{"latex":"E=mc^2"}}]}
            """.trimMargin()

        assertEquals("val a = 1 요약 내용 E=mc^2", tiptap(json))
    }

    @Test
    fun `인라인 스타일로 숨긴 요소는 공백·대소문자·important 차이와 관계없이 제외한다`() {
        val text =
            html(
                """
                <p style="display:none">숨김1</p>
                <p style="display : none">숨김2</p>
                <p style="  DISPLAY:NONE  ">숨김3</p>
                <p style="color:red; display:none !important">숨김4</p>
                <p style="visibility: hidden">숨김5</p>
                <p style="visibility :HIDDEN">숨김6</p>
                <p style="display:block">보임1</p>
                <p style="color:red">보임2</p>
                <p style="display:none-ish">보임3</p>
                """.trimIndent(),
            )

        assertEquals("보임1 보임2 보임3", text)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "display : none; display : block",
            "visibility : hidden; visibility : visible",
            "DISPLAY: NONE ! important; display: block !IMPORTANT",
            "visibility: hidden !important; VISIBILITY: VISIBLE ! important",
        ],
    )
    fun `같은 우선순위에서 마지막 선언이 표시 상태이면 텍스트를 포함한다`(style: String) {
        assertEquals("본문", html("""<p style="$style">본문</p>"""))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "display: block; display: none",
            "visibility: visible; visibility: hidden",
            "display: block !important; display: none !important",
            "visibility: visible !important; visibility: hidden !important",
        ],
    )
    fun `같은 우선순위에서 마지막 선언이 숨김 상태이면 텍스트를 제외한다`(style: String) {
        assertEquals("", html("""<p style="$style">본문</p>"""))
    }

    @ParameterizedTest
    @ValueSource(strings = ["display: none !important; display: block", "visibility: hidden !important; visibility: visible"])
    fun `앞선 숨김 선언에 important가 있으면 뒤의 일반 표시 선언보다 우선한다`(style: String) {
        assertEquals("", html("""<p style="$style">본문</p>"""))
    }

    @ParameterizedTest
    @ValueSource(strings = ["display: block !important; display: none", "visibility: visible !important; visibility: hidden"])
    fun `앞선 표시 선언에 important가 있으면 뒤의 일반 숨김 선언보다 우선한다`(style: String) {
        assertEquals("본문", html("""<p style="$style">본문</p>"""))
    }

    @ParameterizedTest
    @ValueSource(strings = ["display: none; display: block !important", "visibility: hidden; visibility: visible !important"])
    fun `뒤의 표시 선언에 important가 있으면 앞선 일반 숨김 선언보다 우선한다`(style: String) {
        assertEquals("본문", html("""<p style="$style">본문</p>"""))
    }

    @ParameterizedTest
    @ValueSource(strings = ["display: block; display: none !important", "visibility: visible; visibility: hidden !important"])
    fun `뒤의 숨김 선언에 important가 있으면 앞선 일반 표시 선언보다 우선한다`(style: String) {
        assertEquals("", html("""<p style="$style">본문</p>"""))
    }

    @ParameterizedTest
    @ValueSource(strings = ["display: none; visibility: visible", "visibility: hidden; display: block"])
    fun `다른 속성의 표시 선언은 숨김 속성을 덮어쓰지 않는다`(style: String) {
        assertEquals("", html("""<p style="$style">본문</p>"""))
    }

    @Test
    fun `풀어쓴 한글과 악센트 분리 문자는 NFC로 정규화해 추출한다`() {
        val expected = Normalizer.normalize("한글 café", Normalizer.Form.NFC)
        val nfd = Normalizer.normalize("한글 café", Normalizer.Form.NFD)

        assertEquals(expected, html("<p>$nfd</p>"))
        val json = """{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"$nfd"}]}]}"""
        assertEquals(expected, tiptap(json))
    }

    @Test
    fun `NBSP와 전각 공백도 일반 공백 하나로 정리한다`() {
        assertEquals("가 나 다", html("<p>가&nbsp;&nbsp;나　다</p>"))
        val json = """{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"가  나"}]}]}"""
        assertEquals("가 나", tiptap(json))
    }

    @Test
    fun `문장 안의 인라인 수식은 앞뒤 글자와 이어 붙인다`() {
        val json =
            """{"type":"doc","content":[{"type":"paragraph","content":[
                |{"type":"text","text":"값 "},{"type":"inlineMath","attrs":{"latex":"x"}},{"type":"text","text":"입니다"}]}]}
            """.trimMargin()

        assertEquals("값 x입니다", tiptap(json))
    }

    @Test
    fun `Tiptap 미디어·목차 노드는 본문에 포함하지 않는다`() {
        val json =
            """{"type":"doc","content":[
                |{"type":"video","attrs":{"src":"https://v.example/a.mp4"}},
                |{"type":"youtube","attrs":{"src":"https://youtu.be/abc"}},
                |{"type":"audio","attrs":{"src":"https://a.example/a.mp3","title":"오디오제목"}},
                |{"type":"tableOfContents"},
                |{"type":"paragraph","content":[{"type":"text","text":"본문"}]}]}
            """.trimMargin()

        assertEquals("본문", tiptap(json))
    }

    @Test
    fun `깨진 Tiptap JSON은 빈 텍스트로 처리한다`() {
        assertEquals("", tiptap("{깨진"))
    }
}
