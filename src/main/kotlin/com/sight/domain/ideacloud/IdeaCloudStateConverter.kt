package com.sight.domain.ideacloud

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class IdeaCloudStateConverter : AttributeConverter<IdeaCloudState, String> {
    override fun convertToDatabaseColumn(attribute: IdeaCloudState?): String? {
        return attribute?.value
    }

    override fun convertToEntityAttribute(dbData: String?): IdeaCloudState? {
        return dbData?.let { value ->
            IdeaCloudState.entries.find { it.value == value }
        }
    }
}
