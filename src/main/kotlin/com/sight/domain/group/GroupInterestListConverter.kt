package com.sight.domain.group

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

// 관심 분야 목록을 쉼표로 구분한 enum 이름 문자열로 저장한다. null은 null, 빈 목록은 빈 문자열로 저장해 서로 구분한다.
@Converter
class GroupInterestListConverter : AttributeConverter<List<GroupInterest>?, String?> {
    override fun convertToDatabaseColumn(attribute: List<GroupInterest>?): String? =
        attribute?.joinToString(",") { it.name }

    // 저장된 값에 허용하지 않는 이름이 있으면 데이터 오류이므로 예외를 던진다.
    override fun convertToEntityAttribute(dbData: String?): List<GroupInterest>? {
        if (dbData == null) return null
        if (dbData.isEmpty()) return emptyList()
        return dbData.split(",").map { name ->
            requireNotNull(GroupInterest.entries.find { it.name == name }) { "허용하지 않는 관심 분야입니다: $name" }
        }
    }
}
