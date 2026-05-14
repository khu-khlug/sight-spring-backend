package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.Group
import com.sight.domain.group.GroupAccessGrade
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.HtmlUtils

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
    private val groupLogService: GroupLogService,
    private val notificationService: NotificationService,
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
            GroupAccessGrade.PRIVATE -> isMember || group.author == requester.id
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

    @Transactional
    fun delegateMaster(
        groupId: Long,
        requesterId: Long,
        newMasterId: Long,
    ) {
        val group =
            groupRepository.findById(groupId).orElseThrow {
                NotFoundException("그룹을 찾을 수 없습니다")
            }

        if (group.master != requesterId) {
            throw ForbiddenException("그룹장만 위임할 수 있습니다")
        }

        if (newMasterId == requesterId) {
            throw BadRequestException("자기 자신에게 위임할 수 없습니다")
        }

        val newMaster =
            memberRepository.findById(newMasterId).orElseThrow {
                NotFoundException("회원을 찾을 수 없습니다")
            }

        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, newMasterId)) {
            throw BadRequestException("위임 대상이 그룹 멤버가 아닙니다")
        }

        val oldMaster = memberRepository.findById(requesterId).get()

        groupRepository.save(group.copy(master = newMasterId))
        groupRepository.touchChangedAtAndPromoteFromSuspend(groupId)

        val logMessage =
            "그룹장이 ${oldMaster.college} ${oldMaster.realname}에서 " +
                "${newMaster.college} ${newMaster.realname}에게 위임되었습니다."
        groupLogService.createLog(groupId, requesterId, logMessage)

        val notificationContent =
            "<a href=\"/group/$groupId#member\"><u>${HtmlUtils.htmlEscape(group.title)}</u></a> " +
                "그룹장이 ${newMaster.realname}에게 위임되었습니다."
        val memberIds = groupMemberRepository.findByGroupId(groupId).map { it.member }
        memberIds.forEach { memberId ->
            notificationService.createNotification(
                userId = memberId,
                category = NotificationCategory.GROUP,
                title = "",
                content = notificationContent,
            )
        }
    }
}
