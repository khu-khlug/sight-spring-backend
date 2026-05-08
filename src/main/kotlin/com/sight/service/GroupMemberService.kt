package com.sight.service

import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.Group
import com.sight.domain.group.GroupAccessGrade
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class GroupMemberListItem(
    val userId: Long,
    val name: String,
    val realname: String,
    val isLeader: Boolean,
)

data class GroupMemberListResult(
    val members: List<GroupMemberListItem>,
)

@Service
class GroupMemberService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val memberRepository: MemberRepository,
) {
    @Transactional(readOnly = true)
    fun listGroupMembers(
        groupId: Long,
        requesterId: Long,
    ): GroupMemberListResult {
        val group =
            groupRepository.findById(groupId).orElseThrow {
                NotFoundException("그룹을 찾을 수 없습니다")
            }

        val requester = memberRepository.findById(requesterId).get()

        val isMember = groupMemberRepository.existsByGroupIdAndMemberId(groupId, requesterId)
        if (!canViewGroup(group, requester, isMember)) {
            throw ForbiddenException("해당 그룹의 멤버 목록을 조회할 권한이 없습니다")
        }

        val members = groupMemberRepository.findMemberListByGroupId(groupId)
        val sorted =
            members
                .map { it.toItem(masterId = group.master) }
                .sortedWith(compareByDescending<GroupMemberListItem> { it.isLeader }.thenBy { it.userId })

        return GroupMemberListResult(members = sorted)
    }

    private fun canViewGroup(
        group: Group,
        requester: Member,
        isMember: Boolean,
    ): Boolean =
        when (group.grade) {
            GroupAccessGrade.PRIVATE -> isMember
            GroupAccessGrade.MANAGER -> requester.manager
            GroupAccessGrade.MEMBER -> requester.studentStatus != StudentStatus.UNITED
            GroupAccessGrade.ALL -> true
        }

    private fun com.sight.repository.dto.GroupMemberListDto.toItem(masterId: Long): GroupMemberListItem =
        GroupMemberListItem(
            userId = this.userId,
            name = this.name,
            realname = this.realname,
            isLeader = this.userId == masterId,
        )
}
