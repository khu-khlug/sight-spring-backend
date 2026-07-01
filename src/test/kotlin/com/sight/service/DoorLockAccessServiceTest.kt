package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.repository.MemberRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import kotlin.test.assertEquals

class DoorLockAccessServiceTest {
    private val memberRepository: MemberRepository = mock()
    private val pointService: PointService = mock()
    private val service = DoorLockAccessService(memberRepository, pointService)

    @Test
    fun `오늘 첫 출입이면 ExPoint를 적립하고 회원 이름을 반환한다`() {
        val member = member(lastEnter = LocalDateTime.of(2026, 5, 30, 23, 0))
        val number = member.number!!
        given(memberRepository.findByNumber(number)).willReturn(member)

        val result = service.createDoorLockAccess(number, 406, LocalDateTime.of(2026, 5, 31, 10, 0))

        assertEquals(member.realname, result.name)
        verify(pointService).givePoint(member.id, 1, "동방 출입")
        val captor = argumentCaptor<Member>()
        verify(memberRepository).save(captor.capture())
        assertEquals(LocalDateTime.of(2026, 5, 31, 10, 0), captor.firstValue.lastEnter)
    }

    @Test
    fun `같은 날 두 번째 이후 출입은 ExPoint를 적립하지 않는다`() {
        val member = member(lastEnter = LocalDateTime.of(2026, 5, 31, 9, 0))
        val number = member.number!!
        given(memberRepository.findByNumber(number)).willReturn(member)

        val result = service.createDoorLockAccess(number, 406, LocalDateTime.of(2026, 5, 31, 10, 0))

        assertEquals(member.realname, result.name)
        verify(pointService, never()).givePoint(any(), any(), any())
        verify(memberRepository).save(any<Member>())
    }

    @Test
    fun `학번에 해당하는 사용자가 없으면 NotFoundException을 던진다`() {
        given(memberRepository.findByNumber(2026000000L)).willReturn(null)

        assertThrows<NotFoundException> {
            service.createDoorLockAccess(2026000000L, 406)
        }

        verify(memberRepository, never()).save(any<Member>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `학번이나 방 번호가 양수가 아니면 BadRequestException을 던진다`() {
        assertThrows<BadRequestException> {
            service.createDoorLockAccess(0L, 406)
        }
        assertThrows<BadRequestException> {
            service.createDoorLockAccess(2026000000L, 0)
        }

        verify(memberRepository, never()).findByNumber(any())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `KST 자정이 지나면 다시 ExPoint를 적립한다`() {
        val originalMember = member(lastEnter = LocalDateTime.of(2026, 5, 30, 10, 0))
        val number = originalMember.number!!
        given(memberRepository.findByNumber(number)).willReturn(originalMember)

        service.createDoorLockAccess(number, 406, LocalDateTime.of(2026, 5, 31, 23, 59))

        val captor = argumentCaptor<Member>()
        verify(memberRepository).save(captor.capture())
        val firstAccessMember = captor.firstValue
        given(memberRepository.findByNumber(number)).willReturn(firstAccessMember)

        service.createDoorLockAccess(number, 406, LocalDateTime.of(2026, 6, 1, 0, 1))

        verify(pointService, org.mockito.kotlin.times(2)).givePoint(originalMember.id, 1, "동방 출입")
    }

    private fun member(lastEnter: LocalDateTime): Member {
        return Member(
            id = 1L,
            name = "test-user",
            number = 2026000001L,
            realname = "홍길동",
            studentStatus = StudentStatus.UNDERGRADUATE,
            status = UserStatus.ACTIVE,
            lastEnter = lastEnter,
        )
    }
}
