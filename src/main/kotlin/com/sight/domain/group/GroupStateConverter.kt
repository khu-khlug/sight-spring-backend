package com.sight.domain.group

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class GroupStateConverter : AttributeConverter<GroupState, String> {
    override fun convertToDatabaseColumn(attribute: GroupState?): String? {
        return attribute?.value
    }

    override fun convertToEntityAttribute(dbData: String?): GroupState? {
        return dbData?.let { value ->
            GroupState.entries.find { it.value == value }
        }
    }
}
