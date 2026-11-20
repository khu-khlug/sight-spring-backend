package com.sight.domain.group

// 그룹 활동 로그의 이벤트별 로그데이터. 각 이벤트는 자기 로그타입과 그 타입의 필수 필드를 가진다.
// 이름·제목·설명 등은 발생 당시 원문 텍스트이며 HTML·Markdown 링크를 넣지 않는다.
// 그룹·리스트·카드·기록 식별자는 10진 문자열로, 회원 식별자는 숫자로 담는다.
// LEGACY는 신규로 만들 수 없으므로 대응하는 이벤트가 없다.
sealed interface GroupLogData {
    val logType: GroupLogType

    // 로그타입의 필수 필드 이름과 값. 필드가 없는 이벤트는 빈 맵이다.
    fun fields(): Map<String, Any>

    // 그룹

    data class GroupCreated(
        val groupTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.GROUP_CREATED

        override fun fields() = mapOf("groupTitle" to groupTitle)
    }

    // changedFields는 실제 변경한 항목의 표시명이다.
    data class GroupInfoUpdated(
        val changedFields: List<String>,
    ) : GroupLogData {
        override val logType = GroupLogType.GROUP_INFO_UPDATED

        override fun fields() = mapOf("changedFields" to changedFields)
    }

    data object GroupJoinEnabled : GroupLogData {
        override val logType = GroupLogType.GROUP_JOIN_ENABLED

        override fun fields() = emptyMap<String, Any>()
    }

    data object GroupJoinDisabled : GroupLogData {
        override val logType = GroupLogType.GROUP_JOIN_DISABLED

        override fun fields() = emptyMap<String, Any>()
    }

    data class GroupCategoryUpdated(
        val category: GroupCategory,
    ) : GroupLogData {
        override val logType = GroupLogType.GROUP_CATEGORY_UPDATED

        override fun fields() = mapOf("category" to category.name)
    }

    data class GroupDepartmentsSet(
        val departmentNames: List<String>,
    ) : GroupLogData {
        override val logType = GroupLogType.GROUP_DEPARTMENTS_SET

        override fun fields() = mapOf("departmentNames" to departmentNames)
    }

    data object GroupDepartmentsCleared : GroupLogData {
        override val logType = GroupLogType.GROUP_DEPARTMENTS_CLEARED

        override fun fields() = emptyMap<String, Any>()
    }

    data object GroupProgress : GroupLogData {
        override val logType = GroupLogType.GROUP_PROGRESS

        override fun fields() = emptyMap<String, Any>()
    }

    data object GroupEndedSuccess : GroupLogData {
        override val logType = GroupLogType.GROUP_ENDED_SUCCESS

        override fun fields() = emptyMap<String, Any>()
    }

    data object GroupEndedFail : GroupLogData {
        override val logType = GroupLogType.GROUP_ENDED_FAIL

        override fun fields() = emptyMap<String, Any>()
    }

    data object GroupStopped : GroupLogData {
        override val logType = GroupLogType.GROUP_STOPPED

        override fun fields() = emptyMap<String, Any>()
    }

    data object GroupPortfolioUnpublished : GroupLogData {
        override val logType = GroupLogType.GROUP_PORTFOLIO_UNPUBLISHED

        override fun fields() = emptyMap<String, Any>()
    }

    data object GroupPortfolioPublished : GroupLogData {
        override val logType = GroupLogType.GROUP_PORTFOLIO_PUBLISHED

        override fun fields() = emptyMap<String, Any>()
    }

    data object GroupMemberJoined : GroupLogData {
        override val logType = GroupLogType.GROUP_MEMBER_JOINED

        override fun fields() = emptyMap<String, Any>()
    }

    data object GroupMemberLeft : GroupLogData {
        override val logType = GroupLogType.GROUP_MEMBER_LEFT

        override fun fields() = emptyMap<String, Any>()
    }

    data class GroupMemberKicked(
        val targetUserId: Long,
        val targetUserName: String,
        val targetUserCollege: String,
    ) : GroupLogData {
        override val logType = GroupLogType.GROUP_MEMBER_KICKED

        override fun fields() =
            mapOf(
                "targetUserId" to targetUserId,
                "targetUserName" to targetUserName,
                "targetUserCollege" to targetUserCollege,
            )
    }

    data class GroupLeaderDelegated(
        val fromUserId: Long,
        val fromUserName: String,
        val fromUserCollege: String,
        val toUserId: Long,
        val toUserName: String,
        val toUserCollege: String,
    ) : GroupLogData {
        override val logType = GroupLogType.GROUP_LEADER_DELEGATED

        override fun fields() =
            mapOf(
                "fromUserId" to fromUserId,
                "fromUserName" to fromUserName,
                "fromUserCollege" to fromUserCollege,
                "toUserId" to toUserId,
                "toUserName" to toUserName,
                "toUserCollege" to toUserCollege,
            )
    }

    // 리스트

    data class ListCreated(
        val listId: Long,
        val listTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.LIST_CREATED

        override fun fields() = mapOf("listId" to listId.toString(), "listTitle" to listTitle)
    }

    data class ListTitleUpdated(
        val listId: Long,
        val oldTitle: String,
        val newTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.LIST_TITLE_UPDATED

        override fun fields() = mapOf("listId" to listId.toString(), "oldTitle" to oldTitle, "newTitle" to newTitle)
    }

    data class ListDescriptionUpdated(
        val listId: Long,
        val listTitle: String,
        val description: String,
    ) : GroupLogData {
        override val logType = GroupLogType.LIST_DESCRIPTION_UPDATED

        override fun fields() = mapOf("listId" to listId.toString(), "listTitle" to listTitle, "description" to description)
    }

    data class ListDeleted(
        val listId: Long,
        val listTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.LIST_DELETED

        override fun fields() = mapOf("listId" to listId.toString(), "listTitle" to listTitle)
    }

    // 카드

    data class CardCreated(
        val cardId: Long,
        val cardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_CREATED

        override fun fields() = cardFields(cardId, cardTitle)
    }

    data class CardTitleUpdated(
        val cardId: Long,
        val oldTitle: String,
        val newTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_TITLE_UPDATED

        override fun fields() = mapOf("cardId" to cardId.toString(), "oldTitle" to oldTitle, "newTitle" to newTitle)
    }

    data class CardDescriptionAdded(
        val cardId: Long,
        val cardTitle: String,
        val description: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_DESCRIPTION_ADDED

        override fun fields() = cardFields(cardId, cardTitle) + ("description" to description)
    }

    data class CardDescriptionUpdated(
        val cardId: Long,
        val cardTitle: String,
        val description: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_DESCRIPTION_UPDATED

        override fun fields() = cardFields(cardId, cardTitle) + ("description" to description)
    }

    // 드래그와 별도 이동 요청 모두 이 이벤트를 쓴다.
    data class CardMoved(
        val cardId: Long,
        val cardTitle: String,
        val fromListId: Long,
        val fromListTitle: String,
        val toListId: Long,
        val toListTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_MOVED

        override fun fields() =
            cardFields(cardId, cardTitle) +
                mapOf(
                    "fromListId" to fromListId.toString(),
                    "fromListTitle" to fromListTitle,
                    "toListId" to toListId.toString(),
                    "toListTitle" to toListTitle,
                )
    }

    data class CardDeleted(
        val cardId: Long,
        val cardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_DELETED

        override fun fields() = cardFields(cardId, cardTitle)
    }

    data class CardDisabled(
        val cardId: Long,
        val cardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_DISABLED

        override fun fields() = cardFields(cardId, cardTitle)
    }

    data class CardEnabled(
        val cardId: Long,
        val cardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_ENABLED

        override fun fields() = cardFields(cardId, cardTitle)
    }

    data class CardRestored(
        val cardId: Long,
        val cardTitle: String,
        val listId: Long,
        val listTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_RESTORED

        override fun fields() = cardFields(cardId, cardTitle) + mapOf("listId" to listId.toString(), "listTitle" to listTitle)
    }

    data class CardAssigneeCleared(
        val cardId: Long,
        val cardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_ASSIGNEE_CLEARED

        override fun fields() = cardFields(cardId, cardTitle)
    }

    data class CardAssigneeSet(
        val cardId: Long,
        val cardTitle: String,
        val assigneeUserId: Long,
        val assigneeUserName: String,
        val assigneeUserCollege: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_ASSIGNEE_SET

        override fun fields() =
            cardFields(cardId, cardTitle) +
                mapOf(
                    "assigneeUserId" to assigneeUserId,
                    "assigneeUserName" to assigneeUserName,
                    "assigneeUserCollege" to assigneeUserCollege,
                )
    }

    data class CardInheritanceCleared(
        val cardId: Long,
        val cardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_INHERITANCE_CLEARED

        override fun fields() = cardFields(cardId, cardTitle)
    }

    data class CardInheritanceSet(
        val cardId: Long,
        val cardTitle: String,
        val sourceGroupId: Long,
        val sourceGroupTitle: String,
        val sourceCardId: Long,
        val sourceCardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_INHERITANCE_SET

        override fun fields() =
            cardFields(cardId, cardTitle) +
                mapOf(
                    "sourceGroupId" to sourceGroupId.toString(),
                    "sourceGroupTitle" to sourceGroupTitle,
                    "sourceCardId" to sourceCardId.toString(),
                    "sourceCardTitle" to sourceCardTitle,
                )
    }

    // label은 카드 레이블 값이며 색상을 문자열로 보존한다.
    data class CardLabelRemoved(
        val cardId: Long,
        val cardTitle: String,
        val label: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_LABEL_REMOVED

        override fun fields() = cardFields(cardId, cardTitle) + ("label" to label)
    }

    data class CardLabelAdded(
        val cardId: Long,
        val cardTitle: String,
        val label: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_LABEL_ADDED

        override fun fields() = cardFields(cardId, cardTitle) + ("label" to label)
    }

    data class CardPortfolioRemoved(
        val cardId: Long,
        val cardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_PORTFOLIO_REMOVED

        override fun fields() = cardFields(cardId, cardTitle)
    }

    data class CardPortfolioAdded(
        val cardId: Long,
        val cardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_PORTFOLIO_ADDED

        override fun fields() = cardFields(cardId, cardTitle)
    }

    data class CardCoverAdded(
        val cardId: Long,
        val cardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_COVER_ADDED

        override fun fields() = cardFields(cardId, cardTitle)
    }

    data class CardCoverRemoved(
        val cardId: Long,
        val cardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.CARD_COVER_REMOVED

        override fun fields() = cardFields(cardId, cardTitle)
    }

    // 기록

    // contentText는 당시 기록 본문에서 추출한 텍스트이며 문단·줄바꿈을 보존한다.
    data class RecordUpdated(
        val recordId: Long,
        val cardId: Long,
        val cardTitle: String,
        val contentText: String,
    ) : GroupLogData {
        override val logType = GroupLogType.RECORD_UPDATED

        override fun fields() = recordFields(recordId, cardId, cardTitle) + ("contentText" to contentText)
    }

    data class RecordCreated(
        val recordId: Long,
        val cardId: Long,
        val cardTitle: String,
        val contentText: String,
    ) : GroupLogData {
        override val logType = GroupLogType.RECORD_CREATED

        override fun fields() = recordFields(recordId, cardId, cardTitle) + ("contentText" to contentText)
    }

    data class RecordDeleted(
        val recordId: Long,
        val cardId: Long,
        val cardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.RECORD_DELETED

        override fun fields() = recordFields(recordId, cardId, cardTitle)
    }

    data class RecordRestored(
        val recordId: Long,
        val cardId: Long,
        val cardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.RECORD_RESTORED

        override fun fields() = recordFields(recordId, cardId, cardTitle)
    }

    data class RecordPortfolioRemoved(
        val recordId: Long,
        val cardId: Long,
        val cardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.RECORD_PORTFOLIO_REMOVED

        override fun fields() = recordFields(recordId, cardId, cardTitle)
    }

    data class RecordPortfolioAdded(
        val recordId: Long,
        val cardId: Long,
        val cardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.RECORD_PORTFOLIO_ADDED

        override fun fields() = recordFields(recordId, cardId, cardTitle)
    }

    data class RecordMoved(
        val recordId: Long,
        val fromCardId: Long,
        val fromCardTitle: String,
        val toCardId: Long,
        val toCardTitle: String,
    ) : GroupLogData {
        override val logType = GroupLogType.RECORD_MOVED

        override fun fields() =
            mapOf(
                "recordId" to recordId.toString(),
                "fromCardId" to fromCardId.toString(),
                "fromCardTitle" to fromCardTitle,
                "toCardId" to toCardId.toString(),
                "toCardTitle" to toCardTitle,
            )
    }

    // 세미나

    // termSemester는 당시 학기 표시명이다.
    data class SeminarPresentationCancelled(
        val termYear: Int,
        val termSemester: String,
    ) : GroupLogData {
        override val logType = GroupLogType.SEMINAR_PRESENTATION_CANCELLED

        override fun fields() = mapOf("termYear" to termYear, "termSemester" to termSemester)
    }

    data class SeminarPresentationRegistered(
        val termYear: Int,
        val termSemester: String,
    ) : GroupLogData {
        override val logType = GroupLogType.SEMINAR_PRESENTATION_REGISTERED

        override fun fields() = mapOf("termYear" to termYear, "termSemester" to termSemester)
    }

    data class SeminarSessionRemoved(
        val termYear: Int,
        val termSemester: String,
    ) : GroupLogData {
        override val logType = GroupLogType.SEMINAR_SESSION_REMOVED

        override fun fields() = mapOf("termYear" to termYear, "termSemester" to termSemester)
    }
}

private fun cardFields(
    cardId: Long,
    cardTitle: String,
): Map<String, Any> = mapOf("cardId" to cardId.toString(), "cardTitle" to cardTitle)

private fun recordFields(
    recordId: Long,
    cardId: Long,
    cardTitle: String,
): Map<String, Any> = mapOf("recordId" to recordId.toString()) + cardFields(cardId, cardTitle)
