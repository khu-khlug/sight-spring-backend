package com.sight.domain.group

// 그룹 활동 로그타입. 각 값은 로그데이터 JSON 객체가 반드시 가져야 하는 필드를 가진다.
// 필드는 모두 필수이며 null을 허용하지 않는다. 필드가 없는 이벤트의 로그데이터는 빈 객체다.
enum class GroupLogType(val requiredFields: Set<String>) {
    // 새 형식 이전의 로그. 로그데이터는 원문 HTML을 담은 html 필드 하나다. 신규 이벤트를 이 타입으로 저장하지 않는다.
    LEGACY(setOf("html")),

    // 그룹
    GROUP_CREATED(setOf("groupTitle")),
    GROUP_INFO_UPDATED(setOf("changedFields")),
    GROUP_JOIN_ENABLED(emptySet()),
    GROUP_JOIN_DISABLED(emptySet()),
    GROUP_CATEGORY_UPDATED(setOf("category")),
    GROUP_DEPARTMENTS_SET(setOf("departmentNames")),
    GROUP_DEPARTMENTS_CLEARED(emptySet()),
    GROUP_PROGRESS(emptySet()),
    GROUP_ENDED_SUCCESS(emptySet()),
    GROUP_ENDED_FAIL(emptySet()),
    GROUP_STOPPED(emptySet()),
    GROUP_PORTFOLIO_UNPUBLISHED(emptySet()),
    GROUP_PORTFOLIO_PUBLISHED(emptySet()),
    GROUP_MEMBER_JOINED(emptySet()),
    GROUP_MEMBER_LEFT(emptySet()),
    GROUP_MEMBER_KICKED(setOf("targetUserId", "targetUserName", "targetUserCollege")),
    GROUP_LEADER_DELEGATED(
        setOf("fromUserId", "fromUserName", "fromUserCollege", "toUserId", "toUserName", "toUserCollege"),
    ),

    // 리스트
    LIST_CREATED(setOf("listId", "listTitle")),
    LIST_TITLE_UPDATED(setOf("listId", "oldTitle", "newTitle")),
    LIST_DESCRIPTION_UPDATED(setOf("listId", "listTitle", "description")),
    LIST_DELETED(setOf("listId", "listTitle")),

    // 카드
    CARD_CREATED(setOf("cardId", "cardTitle")),
    CARD_TITLE_UPDATED(setOf("cardId", "oldTitle", "newTitle")),
    CARD_DESCRIPTION_ADDED(setOf("cardId", "cardTitle", "description")),
    CARD_DESCRIPTION_UPDATED(setOf("cardId", "cardTitle", "description")),
    CARD_MOVED(setOf("cardId", "cardTitle", "fromListId", "fromListTitle", "toListId", "toListTitle")),
    CARD_DELETED(setOf("cardId", "cardTitle")),
    CARD_DISABLED(setOf("cardId", "cardTitle")),
    CARD_ENABLED(setOf("cardId", "cardTitle")),
    CARD_RESTORED(setOf("cardId", "cardTitle", "listId", "listTitle")),
    CARD_ASSIGNEE_CLEARED(setOf("cardId", "cardTitle")),
    CARD_ASSIGNEE_SET(setOf("cardId", "cardTitle", "assigneeUserId", "assigneeUserName", "assigneeUserCollege")),
    CARD_INHERITANCE_CLEARED(setOf("cardId", "cardTitle")),
    CARD_INHERITANCE_SET(
        setOf("cardId", "cardTitle", "sourceGroupId", "sourceGroupTitle", "sourceCardId", "sourceCardTitle"),
    ),
    CARD_LABEL_REMOVED(setOf("cardId", "cardTitle", "label")),
    CARD_LABEL_ADDED(setOf("cardId", "cardTitle", "label")),
    CARD_PORTFOLIO_REMOVED(setOf("cardId", "cardTitle")),
    CARD_PORTFOLIO_ADDED(setOf("cardId", "cardTitle")),
    CARD_COVER_ADDED(setOf("cardId", "cardTitle")),
    CARD_COVER_REMOVED(setOf("cardId", "cardTitle")),

    // 기록
    RECORD_UPDATED(setOf("recordId", "cardId", "cardTitle", "contentText")),
    RECORD_CREATED(setOf("recordId", "cardId", "cardTitle", "contentText")),
    RECORD_DELETED(setOf("recordId", "cardId", "cardTitle")),
    RECORD_RESTORED(setOf("recordId", "cardId", "cardTitle")),
    RECORD_PORTFOLIO_REMOVED(setOf("recordId", "cardId", "cardTitle")),
    RECORD_PORTFOLIO_ADDED(setOf("recordId", "cardId", "cardTitle")),
    RECORD_MOVED(setOf("recordId", "fromCardId", "fromCardTitle", "toCardId", "toCardTitle")),

    // 세미나
    SEMINAR_PRESENTATION_CANCELLED(setOf("termYear", "termSemester")),
    SEMINAR_PRESENTATION_REGISTERED(setOf("termYear", "termSemester")),
    SEMINAR_SESSION_REMOVED(setOf("termYear", "termSemester")),
    ;

    // 로그데이터 객체의 필드 이름 중 빠진 필수 필드. 비어 있으면 필수 필드를 모두 갖춘 것이다.
    fun missingFields(fieldNames: Set<String>): Set<String> = requiredFields - fieldNames
}
