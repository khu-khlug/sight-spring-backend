package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.domain.groupmatching.GroupMatchingOption
import com.sight.domain.groupmatching.GroupMatchingType
import com.sight.repository.GroupMatchingOptionRepository
import com.sight.repository.GroupMatchingRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.util.Optional

class GroupMatchingOptionServiceTest {
    private val groupMatchingOptionRepository = mock<GroupMatchingOptionRepository>()
    private val groupMatchingRepository = mock<GroupMatchingRepository>()
    private lateinit var service: GroupMatchingOptionService

    @BeforeEach
    fun setUp() {
        service = GroupMatchingOptionService(groupMatchingOptionRepository, groupMatchingRepository)
    }

    private val groupMatchingId = "gm-1"

    @Test
    fun `listOptions는 해당 그룹 매칭의 옵션 목록을 반환한다`() {
        // given
        val type = GroupMatchingType.BASIC_LANGUAGE_STUDY
        val option =
            GroupMatchingOption(
                id = "opt-1",
                groupMatchingId = groupMatchingId,
                name = "Java",
                groupMatchingType = type,
            )

        given(groupMatchingRepository.findById(groupMatchingId))
            .willReturn(Optional.of(mock()))
        given(groupMatchingOptionRepository.findAllByGroupMatchingIdAndGroupMatchingType(groupMatchingId, type))
            .willReturn(listOf(option))

        // when
        val result = service.listOptions(groupMatchingId, type)

        // then
        assertEquals(1, result.size)
        assertEquals("Java", result[0].name)
    }

    @Test
    fun `listOptions는 그룹 매칭이 존재하지 않으면 NotFoundException을 던진다`() {
        // given
        given(groupMatchingRepository.findById(groupMatchingId))
            .willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            service.listOptions(groupMatchingId, GroupMatchingType.BASIC_LANGUAGE_STUDY)
        }
    }
}
