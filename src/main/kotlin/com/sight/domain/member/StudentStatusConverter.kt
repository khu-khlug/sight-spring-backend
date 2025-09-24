package com.sight.domain.member

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class StudentStatusConverter : AttributeConverter<StudentStatus, Int> {
    override fun convertToDatabaseColumn(attribute: StudentStatus?): Int? {
        return attribute?.code
    }

    override fun convertToEntityAttribute(dbData: Int?): StudentStatus? {
        if (dbData == null) return null
        return StudentStatus.entries.find { it.code == dbData }
            ?: throw IllegalArgumentException("유효하지 않은 StudentStatus 코드: $dbData")
    }
}
