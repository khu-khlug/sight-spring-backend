package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.repository.GroupMatchingAnswerFieldRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingFieldRepository
import com.sight.repository.GroupMatchingSubjectRepository
import com.sight.repository.MatchedGroupRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock

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
    fun `getAllAnswers는 groupType이 STUDY나 PROJECT가 아니면 에러를 던진다`() {
        // given
        val groupMatchingId = "gm-1"

        // when & then
        assertThrows<BadRequestException> {
            service.getAllAnswers(groupMatchingId, groupType = "INVALID")
        }
    }

    @Test
    fun `getAllAnswers는 fieldId가 유효하지 않으면 에러를 던진다`() {
        // given
        val groupMatchingId = "gm-1"
        val invalidFieldId = "invalid-field"
        given(fieldRepository.existsById(invalidFieldId)).willReturn(false)

        // when & then
        assertThrows<BadRequestException> {
            service.getAllAnswers(groupMatchingId, fieldId = invalidFieldId)
        }
    }

    @Test
    fun `getAllAnswers는 offset이 음수이면 에러를 던진다`() {
        // given
        val groupMatchingId = "gm-1"

        // when & then
        assertThrows<BadRequestException> {
            service.getAllAnswers(groupMatchingId, offset = -1)
        }
    }

    @Test
    fun `getAllAnswers는 limit이 0 이하이면 에러를 던진다`() {
        // given
        val groupMatchingId = "gm-1"

        // when & then
        assertThrows<BadRequestException> {
            service.getAllAnswers(groupMatchingId, limit = 0)
        }

        assertThrows<BadRequestException> {
            service.getAllAnswers(groupMatchingId, limit = -1)
        }
    }
}
