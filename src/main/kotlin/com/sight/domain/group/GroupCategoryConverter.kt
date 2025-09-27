package com.sight.domain.group

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class GroupCategoryConverter : AttributeConverter<GroupCategory, String> {
    override fun convertToDatabaseColumn(attribute: GroupCategory?): String? {
        return attribute?.value
    }

    override fun convertToEntityAttribute(dbData: String?): GroupCategory? {
        return dbData?.let { value ->
            GroupCategory.entries.find { it.value == value }
        }
    }
}
