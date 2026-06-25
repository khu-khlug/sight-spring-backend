package com.sight.domain.schedule

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScheduleCategoryTest {
    @Test
    fun `GROUP_ACTIVITY는 그룹 활동 tier로 분류된다`() {
        val category = ScheduleCategory.GROUP_ACTIVITY

        assertTrue(category.isGroupActivity)
        assertFalse(category.isSeminar)
        assertFalse(category.isManagerCategory)
    }

    @Test
    fun `SEMINAR는 세미나 tier로 분류된다`() {
        val category = ScheduleCategory.SEMINAR

        assertTrue(category.isSeminar)
        assertFalse(category.isGroupActivity)
        assertFalse(category.isManagerCategory)
    }

    @Test
    fun `CLUB ACADEMIC EXTERNAL MANAGEMENT AFTERPARTY OTHER는 운영진 tier로 분류된다`() {
        val managerCategories =
            listOf(
                ScheduleCategory.CLUB,
                ScheduleCategory.ACADEMIC,
                ScheduleCategory.EXTERNAL,
                ScheduleCategory.MANAGEMENT,
                ScheduleCategory.AFTERPARTY,
                ScheduleCategory.OTHER,
            )

        managerCategories.forEach { category ->
            assertTrue(category.isManagerCategory, "$category 는 운영진 카테고리여야 한다")
            assertFalse(category.isGroupActivity, "$category 는 그룹 활동이 아니어야 한다")
            assertFalse(category.isSeminar, "$category 는 세미나가 아니어야 한다")
        }
    }

    @Test
    fun `모든 카테고리는 정확히 하나의 tier에만 속한다`() {
        ScheduleCategory.entries.forEach { category ->
            val matched =
                listOf(category.isGroupActivity, category.isSeminar, category.isManagerCategory)
                    .count { it }

            assertTrue(matched == 1, "$category 는 정확히 하나의 tier에 속해야 한다 (matched=$matched)")
        }
    }
}
