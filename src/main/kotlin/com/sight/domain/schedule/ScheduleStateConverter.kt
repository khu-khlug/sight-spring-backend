package com.sight.domain.schedule

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class ScheduleStateConverter : AttributeConverter<ScheduleState, String> {
    override fun convertToDatabaseColumn(attribute: ScheduleState): String = attribute.name

    override fun convertToEntityAttribute(dbData: String): ScheduleState = ScheduleState.valueOf(dbData)
}
