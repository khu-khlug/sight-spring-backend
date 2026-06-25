package com.sight.service

import com.sight.domain.member.Member
import com.sight.repository.MemberRepository
import org.springframework.stereotype.Service

@Service
class DoorLockMemberService(
    private val memberRepository: MemberRepository,
) {
    /**
     * 도어락 출입 검증 대상 회원 명단을 조회한다.
     *
     * 학번(`number`)이 없는 회원은 출입 검증 대상이 될 수 없으므로 제외한다.
     * 제명·탈퇴 회원은 학번이 0으로 초기화되므로(null 아님) 함께 제외한다.
     */
    fun listDoorLockMembers(): List<Member> = memberRepository.findByNumberIsNotNull().filter { it.number != 0L }
}
