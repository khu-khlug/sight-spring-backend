package com.sight.core.id

import com.sight.controllers.http.dto.CreateGroupActivityScheduleRequest
import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import kotlin.test.assertTrue

class IntegerIdTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `레거시의 작은 정수 식별자를 받는다`() {
        assertTrue(violationsOf("1000000").isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "0", "-1", "01", "1.0", "abc", "9223372036854775808"])
    fun `양의 Long 10진 표기가 아닌 식별자는 거부한다`(groupId: String) {
        assertTrue(violationsOf(groupId).isNotEmpty())
    }

    private fun violationsOf(groupId: String) =
        validator.validate(
            CreateGroupActivityScheduleRequest(
                title = "정기 모임",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 11, 10, 19, 0),
                endAt = LocalDateTime.of(2026, 11, 10, 21, 0),
                groupId = groupId,
            ),
        )
}
