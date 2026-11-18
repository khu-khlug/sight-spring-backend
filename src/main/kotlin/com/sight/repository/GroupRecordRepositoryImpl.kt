package com.sight.repository

import com.sight.domain.group.GroupRecord
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class GroupRecordRepositoryImpl : GroupRecordRepositoryCustom {
    override fun delete(
        record: GroupRecord,
        at: Instant,
    ) {
        record.delete(at)
    }
}
