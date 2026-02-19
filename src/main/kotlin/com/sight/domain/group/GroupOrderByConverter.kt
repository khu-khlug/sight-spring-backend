package com.sight.domain.group

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class GroupOrderByConverter : Converter<String, GroupOrderBy> {
    override fun convert(source: String): GroupOrderBy? = GroupOrderBy.fromValue(source)
}
