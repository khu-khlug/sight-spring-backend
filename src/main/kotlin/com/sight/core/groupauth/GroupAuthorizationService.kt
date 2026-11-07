package com.sight.core.groupauth

import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.repository.GroupAuthorizationRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupAuthorizationService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupAuthorizationRepository: GroupAuthorizationRepository,
) {
    @Transactional(readOnly = true)
    fun requirePermission(
        groupId: Long,
        recordId: Long?,
        requesterId: Long,
        isManager: Boolean,
        roles: Set<GroupRole>,
        allowManager: Boolean,
    ) {
        // 운영진 예외는 그룹 권한 판정만 생략한다. 업무 서비스의 대상 존재·상태 검사는 그대로 적용된다.
        if (allowManager && isManager) return
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        // OR 판정에서 실제로 필요한 정보만 조회하고, 여러 권한 분기가 사용해도 요청당 한 번만 조회한다.
        val isMember by lazy { groupMemberRepository.existsByGroupIdAndMemberId(groupId, requesterId) }
        val hasMemberPermission by lazy { group.isLeader(requesterId) || isMember }
        val recordAuthor by lazy { recordId?.let(groupAuthorizationRepository::findRecordAuthorById) }
        val isManageGroup by lazy { groupAuthorizationRepository.isManageGroup(groupId) }
        // 한 분기의 대상이 없어도 다른 권한으로 허용될 수 있으므로, 404는 모든 분기가 실패한 뒤 결정한다.
        var missingRecord = false
        var missingMember = false

        val allowed =
            roles.any { role ->
                when (role) {
                    GroupRole.LEADER -> group.isLeader(requesterId)
                    GroupRole.MEMBER -> hasMemberPermission
                    GroupRole.MANAGER -> isManager && isManageGroup
                    GroupRole.VIEWER -> {
                        when (group.visibility) {
                            // 그룹 멤버만
                            GroupVisibility.PRIVATE -> hasMemberPermission
                            // 운영진만
                            GroupVisibility.MANAGER_ONLY -> isManager
                            // 그룹 멤버 또는 교류 회원을 제외한 쿠러그 회원
                            GroupVisibility.MEMBER_ONLY ->
                                hasMemberPermission ||
                                    run {
                                        val unitedMember = groupAuthorizationRepository.isUnitedMember(requesterId)
                                        if (unitedMember == null) missingMember = true
                                        unitedMember == false
                                    }
                            // @GroupAuth는 @Auth([UserRole.USER, UserRole.MANAGER])가 선행되어야하므로 로그인 회원 여부는 이미 확인되었다.
                            GroupVisibility.PUBLIC -> true
                        }
                    }
                    GroupRole.WRITER -> {
                        // lazy 위임 프로퍼티는 스마트 캐스트할 수 없어 지역 val로 받아 null 검사 후 접근한다.
                        val author = recordAuthor
                        if (author == null) missingRecord = true
                        author != null && author.groupId == groupId && hasMemberPermission && author.authorMemberId == requesterId
                    }
                }
            }
        if (allowed) return
        if (missingRecord) throw NotFoundException("기록을 찾을 수 없습니다.")
        if (missingMember) throw NotFoundException("회원을 찾을 수 없습니다.")
        throw ForbiddenException("그룹에 대한 권한이 부족합니다.")
    }
}
