package com.sight.domain.file

enum class FileReferenceType {
    GROUP_RECORD_ATTACHMENT,
    GROUP_RECORD_DRAFT_ATTACHMENT,
    GROUP_CARD_COVER,
    GROUP_ACTIVITY_REPORT,
    ;

    val isSingleOwnerFile: Boolean
        get() = this == GROUP_CARD_COVER || this == GROUP_ACTIVITY_REPORT
}
