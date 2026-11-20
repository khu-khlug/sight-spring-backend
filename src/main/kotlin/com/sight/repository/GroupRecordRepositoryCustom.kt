package com.sight.repository

import com.sight.domain.group.GroupRecord
import java.time.Instant

interface GroupRecordRepositoryCustom {
    fun delete(
        record: GroupRecord,
        at: Instant,
    )
}
