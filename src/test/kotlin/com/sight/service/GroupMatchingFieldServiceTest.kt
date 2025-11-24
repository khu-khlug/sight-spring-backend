package com.sight.service

import com.sight.controllers.http.dto.AddGroupMatchingFieldRequest
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.groupmatching.GroupMatchingField
import com.sight.repository.GroupMatchingFieldRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class GroupMatchingFieldServiceTest {
    private val groupMatchingFieldRepository = mock<GroupMatchingFieldRepository>()
    private val groupMatchingFieldService = GroupMatchingFieldService(groupMatchingFieldRepository)

    @Test
    fun `addGroupMatchingField는 새로운 관심분야를 생성하고 저장한다`() {
        // given
        val request = AddGroupMatchingFieldRequest(fieldName = "백엔드")

        given(groupMatchingFieldRepository.existsByName(request.fieldName)).willReturn(false)
        given(groupMatchingFieldRepository.save(any<GroupMatchingField>())).willAnswer {
            val savedField = it.arguments[0] as GroupMatchingField
            // ID는 랜덤 생성되므로 이름만 검증하거나, save된 객체를 그대로 반환한다고 가정
            savedField
        }

        // when
        val result = groupMatchingFieldService.addGroupMatchingField(request)

        // then
        assertEquals(request.fieldName, result.name)
        verify(groupMatchingFieldRepository).existsByName(request.fieldName)
        verify(groupMatchingFieldRepository).save(any<GroupMatchingField>())
    }

    @Test
    fun `addGroupMatchingField는 이름이 이미 존재하면 UnprocessableEntityException을 던진다`() {
        // given
        val request = AddGroupMatchingFieldRequest(fieldName = "백엔드")
        given(groupMatchingFieldRepository.existsByName(request.fieldName)).willReturn(true)

        // when & then
        assertThrows<UnprocessableEntityException> {
            groupMatchingFieldService.addGroupMatchingField(request)
        }
        verify(groupMatchingFieldRepository).existsByName(request.fieldName)
    }
}
