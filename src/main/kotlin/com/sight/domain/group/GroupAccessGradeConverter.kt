package com.sight.domain.group

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class GroupAccessGradeConverter : AttributeConverter<GroupAccessGrade, Int> {
    override fun convertToDatabaseColumn(attribute: GroupAccessGrade?): Int? {
        return attribute?.code
    }

    override fun convertToEntityAttribute(dbData: Int?): GroupAccessGrade? {
        return dbData?.let { code ->
            GroupAccessGrade.entries.find { it.code == code }
        }
    }
}
