package com.sight.service

import com.sight.controllers.http.dto.AddGroupMatchingFieldRequest
import com.sight.core.auth.UserRole
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.groupmatching.GroupMatchingField
import com.sight.repository.GroupMatchingFieldRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import java.util.Optional

class GroupMatchingFieldServiceTest {
    private val groupMatchingFieldRepository = mock<GroupMatchingFieldRepository>()
    private val groupMatchingFieldService = GroupMatchingFieldService(groupMatchingFieldRepository)

    @Test
    fun `addGroupMatchingField는 새로운 관심분야를 생성하고 저장한다`() {
        // given
        val request = AddGroupMatchingFieldRequest(fieldName = "백엔드")

        given(groupMatchingFieldRepository.findByName(request.fieldName)).willReturn(null)
        given(groupMatchingFieldRepository.save(any<GroupMatchingField>())).willAnswer {
            it.arguments[0] as GroupMatchingField
        }

        // when
        val result = groupMatchingFieldService.addGroupMatchingField(request.fieldName)

        // then
        assertEquals(request.fieldName, result.name)
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
            groupMatchingFieldService.addGroupMatchingField(request.fieldName)
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
        val result = groupMatchingFieldService.addGroupMatchingField(request.fieldName)

        // then
        assertEquals("백엔드", result.name)
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
        given(groupMatchingFieldRepository.findById(fieldId))
            .willReturn(Optional.of(obsoletedField))

        // when & then
        assertThrows<NotFoundException> {
            groupMatchingFieldService.deleteGroupMatchingField(fieldId)
        }
        verify(groupMatchingFieldRepository).findById(fieldId)
        verify(groupMatchingFieldRepository, never()).save(any())
    }

    @Test
    fun `USER 권한인 경우 삭제되지 않은 필드만 조회하는 메소드를 호출한다`() {
        // given
        val activeField =
            GroupMatchingField(
                id = "field-1",
                name = "Active Field",
                obsoletedAt = null,
            )
        // USER 요청 시 findAllByObsoletedAtIsNull()이 호출될 것임
        given(groupMatchingFieldRepository.findAllByObsoletedAtIsNull())
            .willReturn(listOf(activeField))

        // when
        val result = groupMatchingFieldService.getGroupMatchingFields(UserRole.USER)

        // then
        assertEquals(1, result.size)
        assertEquals(activeField.id, result[0].id)
        assertEquals(activeField.name, result[0].name)

        // [핵심 검증] 올바른 리포지토리 메서드가 호출되었는지 확인
        verify(groupMatchingFieldRepository).findAllByObsoletedAtIsNull()
        verify(groupMatchingFieldRepository, never()).findAll()
    }

    @Test
    fun `MANAGER 권한인 경우 모든 필드를 조회하는 메소드를 호출한다`() {
        // given
        val activeField =
            GroupMatchingField(
                id = "field-1",
                name = "Active Field",
                obsoletedAt = null,
            )
        val obsoletedField =
            GroupMatchingField(
                id = "field-2",
                name = "Obsoleted Field",
                obsoletedAt = LocalDateTime.now(),
            )

        // MANAGER 요청 시 findAll()이 호출될 것임
        given(groupMatchingFieldRepository.findAll())
            .willReturn(listOf(activeField, obsoletedField))

        // when
        val result = groupMatchingFieldService.getGroupMatchingFields(UserRole.MANAGER)

        // then
        assertEquals(2, result.size)
        assertEquals(activeField.id, result[0].id)
        assertEquals(obsoletedField.id, result[1].id)
        // [핵심 검증] findAll()이 호출되었는지 확인
        verify(groupMatchingFieldRepository).findAll()
        // [핵심 검증] findAllByObsoletedAtIsNull()은 호출되지 않았는지 확인
        verify(groupMatchingFieldRepository, never()).findAllByObsoletedAtIsNull()
    }
}
