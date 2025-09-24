package com.sight.domain.member

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class UserStatusConverter : AttributeConverter<UserStatus, Int> {
    override fun convertToDatabaseColumn(attribute: UserStatus?): Int? {
        return attribute?.code?.toInt()
    }

    override fun convertToEntityAttribute(dbData: Int?): UserStatus? {
        if (dbData == null) return null
        return UserStatus.entries.find { it.code == dbData.toLong() }
            ?: throw IllegalArgumentException("유효하지 않은 UserStatus 코드: $dbData")
    }
}
