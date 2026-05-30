package com.sight.service

import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.repository.MemberRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertEquals

class DoorLockMemberServiceTest {
    private val memberRepository = mock<MemberRepository>()
    private val doorLockMemberService = DoorLockMemberService(memberRepository)

    private fun member(
        id: Long,
        realname: String,
        number: Long?,
    ): Member =
        Member(
            id = id,
            name = "user$id",
            realname = realname,
            number = number,
            studentStatus = StudentStatus.UNDERGRADUATE,
            status = UserStatus.ACTIVE,
        )

    @Test
    fun `학번이 있는 회원 명단을 반환한다`() {
        given(memberRepository.findByNumberIsNotNull()).willReturn(
            listOf(
                member(1L, "김철수", 2020001L),
                member(2L, "이영희", 2020002L),
            ),
        )

        val result = doorLockMemberService.listDoorLockMembers()

        verify(memberRepository).findByNumberIsNotNull()
        assertEquals(2, result.size)
        assertEquals(listOf("김철수", "이영희"), result.map { it.realname })
    }

    @Test
    fun `학번이 0인 제명·탈퇴 회원은 명단에서 제외한다`() {
        given(memberRepository.findByNumberIsNotNull()).willReturn(
            listOf(
                member(1L, "김철수", 2020001L),
                member(2L, "탈퇴회원", 0L),
            ),
        )

        val result = doorLockMemberService.listDoorLockMembers()

        assertEquals(1, result.size)
        assertEquals(2020001L, result.single().number)
    }
}
