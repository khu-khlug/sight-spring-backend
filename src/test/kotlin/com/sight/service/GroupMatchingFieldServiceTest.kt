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

<<<<<<< Updated upstream
        given(groupMatchingFieldRepository.existsByNameAndObsoletedAtIsNull(request.fieldName)).willReturn(false)
        given(groupMatchingFieldRepository.save(any<GroupMatchingField>())).willAnswer {
            val savedField = it.arguments[0] as GroupMatchingField
            // ID는 랜덤 생성되므로 이름만 검증하거나, save된 객체를 그대로 반환한다고 가정
            savedField
        }

        // when
        val result = groupMatchingFieldService.addGroupMatchingField(request)

        // then
        assertEquals(request.fieldName, result.name)
        verify(groupMatchingFieldRepository).existsByNameAndObsoletedAtIsNull(request.fieldName)
        verify(groupMatchingFieldRepository).save(any<GroupMatchingField>())
    }

    @Test
    fun `addGroupMatchingField는 이름이 이미 존재하면 UnprocessableEntityException을 던진다`() {
        // given
        val request = AddGroupMatchingFieldRequest(fieldName = "백엔드")
        given(groupMatchingFieldRepository.existsByNameAndObsoletedAtIsNull(request.fieldName)).willReturn(true)

        // when & then
        assertThrows<UnprocessableEntityException> {
            groupMatchingFieldService.addGroupMatchingField(request)
        }
        verify(groupMatchingFieldRepository).existsByNameAndObsoletedAtIsNull(request.fieldName)
    }

    @Test
    fun `addGroupMatchingField는 삭제된 필드와 같은 이름으로 새 관심분야를 생성할 수 있다`() {
        // given
        val request = AddGroupMatchingFieldRequest(fieldName = "백엔드")

        given(groupMatchingFieldRepository.existsByNameAndObsoletedAtIsNull(request.fieldName)).willReturn(false)
=======
        given(groupMatchingFieldRepository.findByName(request.fieldName)).willReturn(null)
>>>>>>> Stashed changes
        given(groupMatchingFieldRepository.save(any<GroupMatchingField>())).willAnswer {
            it.arguments[0] as GroupMatchingField
        }

        // when
        val result = groupMatchingFieldService.addGroupMatchingField(request)

        // then
        assertEquals(request.fieldName, result.name)
<<<<<<< Updated upstream
        verify(groupMatchingFieldRepository).existsByNameAndObsoletedAtIsNull(request.fieldName)
        verify(groupMatchingFieldRepository).save(any<GroupMatchingField>())
    }
=======
        verify(groupMatchingFieldRepository).findByName(request.fieldName)
        verify(groupMatchingFieldRepository).save(any<GroupMatchingField>())
    }

    @Test
    fun `addGroupMatchingField는 이름이 이미 존재하면 UnprocessableEntityException을 던진다`() {
        // given
        val request = AddGroupMatchingFieldRequest(fieldName = "백엔드")
        val existingField = GroupMatchingField(id = "existing-id", name = "백엔드")
        given(groupMatchingFieldRepository.findByName(request.fieldName)).willReturn(existingField)

        // when & then
        assertThrows<UnprocessableEntityException> {
            groupMatchingFieldService.addGroupMatchingField(request)
        }
        verify(groupMatchingFieldRepository).findByName(request.fieldName)
    }

    @Test
    fun `addGroupMatchingField는 obsoleted된 관심분야를 재활성화한다`() {
        // given
        val request = AddGroupMatchingFieldRequest(fieldName = "백엔드")
        val obsoletedField =
            GroupMatchingField(
                id = "obsoleted-id",
                name = "백엔드",
                obsoletedAt = LocalDateTime.now(),
            )
        given(groupMatchingFieldRepository.findByName(request.fieldName)).willReturn(obsoletedField)
        given(groupMatchingFieldRepository.save(any<GroupMatchingField>())).willAnswer {
            it.arguments[0] as GroupMatchingField
        }

        // when
        val result = groupMatchingFieldService.addGroupMatchingField(request)

        // then
        assertEquals("백엔드", result.name)
        assertEquals(null, result.obsoletedAt)
        verify(groupMatchingFieldRepository).findByName(request.fieldName)
        verify(groupMatchingFieldRepository).save(obsoletedField)
    }

    @Test
    fun `deleteGroupMatchingField는 존재하는 활성 관심분야를 soft delete한다`() {
        // given
        val fieldId = "field-123"
        val field =
            GroupMatchingField(
                id = fieldId,
                name = "백엔드",
            )

        given(groupMatchingFieldRepository.findById(fieldId)).willReturn(Optional.of(field))
        given(groupMatchingFieldRepository.save(any<GroupMatchingField>())).willAnswer {
            it.arguments[0] as GroupMatchingField
        }

        // when
        groupMatchingFieldService.deleteGroupMatchingField(fieldId)

        // then
        val captor = argumentCaptor<GroupMatchingField>()
        verify(groupMatchingFieldRepository).save(captor.capture())
        assertNotNull(captor.firstValue.obsoletedAt) // obsoletedAt이 설정됨
    }

    @Test
    fun `deleteGroupMatchingField는 존재하지 않는 관심분야면 NotFoundException을 던진다`() {
        // given
        val fieldId = "non-existent"
        given(groupMatchingFieldRepository.findById(fieldId)).willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            groupMatchingFieldService.deleteGroupMatchingField(fieldId)
        }
        verify(groupMatchingFieldRepository).findById(fieldId)
        verify(groupMatchingFieldRepository, never()).save(any())
    }

    @Test
    fun `deleteGroupMatchingField는 이미 obsoleted된 관심분야면 NotFoundException을 던진다`() {
        // given
        val fieldId = "obsoleted-field"
        val obsoletedField =
            GroupMatchingField(
                id = fieldId,
                name = "백엔드",
                obsoletedAt = LocalDateTime.now(),
            )
        given(groupMatchingFieldRepository.findById(fieldId)).willReturn(Optional.of(obsoletedField))

        // when & then
        assertThrows<NotFoundException> {
            groupMatchingFieldService.deleteGroupMatchingField(fieldId)
        }
        verify(groupMatchingFieldRepository).findById(fieldId)
        verify(groupMatchingFieldRepository, never()).save(any())
    }
>>>>>>> Stashed changes
}
