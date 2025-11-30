package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.groupmatching.GroupMatchingField
import com.sight.repository.GroupMatchingAnswerFieldRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingFieldRepository
import com.sight.repository.GroupMatchingSubjectRepository
import com.sight.repository.MatchedGroupRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.time.LocalDateTime
import java.util.Optional

class GroupMatchingAnswerServiceTest {
    private val answerRepository = mock<GroupMatchingAnswerRepository>()
    private val answerFieldRepository = mock<GroupMatchingAnswerFieldRepository>()
    private val subjectRepository = mock<GroupMatchingSubjectRepository>()
    private val matchedGroupRepository = mock<MatchedGroupRepository>()
    private val fieldRepository = mock<GroupMatchingFieldRepository>()

    private val service =
        GroupMatchingAnswerService(
            answerRepository,
            answerFieldRepository,
            subjectRepository,
            matchedGroupRepository,
            fieldRepository,
        )

    @Test
    fun `getAllAnswers는 fieldId가 존재하지 않으면 에러를 던진다`() {
        // given
        val groupMatchingId = "gm-1"
        val invalidFieldId = "invalid-field"
        given(fieldRepository.findById(invalidFieldId)).willReturn(Optional.empty())

        // when & then
        assertThrows<BadRequestException> {
            service.getAllAnswers(groupMatchingId, fieldId = invalidFieldId)
        }
    }

    @Test
    fun `getAllAnswers는 obsoleted된 필드이면 에러를 던진다`() {
        // given
        val groupMatchingId = "gm-1"
        val obsoletedFieldId = "obsoleted-field"
        val obsoletedField =
            GroupMatchingField(
                id = obsoletedFieldId,
                name = "폐기된 분야",
                createdAt = LocalDateTime.now().minusDays(30),
                obsoletedAt = LocalDateTime.now().minusDays(1),
            )
        given(fieldRepository.findById(obsoletedFieldId)).willReturn(Optional.of(obsoletedField))

        // when & then
        assertThrows<BadRequestException> {
            service.getAllAnswers(groupMatchingId, fieldId = obsoletedFieldId)
        }
    }
}
