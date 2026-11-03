package com.sight.domain.group

enum class GroupRecordType {
    // TIPTAP 문서 JSON 직렬화 문자열. 새로 작성하는 기록은 모두 이 형식이다.
    TIPTAP,

    // 레거시 HTML. 읽기만 가능하며 수정할 수 없다.
    LEGACY,
}
