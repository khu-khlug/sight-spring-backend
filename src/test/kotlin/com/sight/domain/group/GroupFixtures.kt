package com.sight.domain.group

import com.sight.core.groupauth.GroupVisibility
import java.time.Instant

// 테스트에서 저장된 상태의 그룹 도메인을 만든다. DB가 채번하는 id는 리플렉션으로 넣고, 나머지 상태는 도메인 메서드로 만든다.
fun testGroup(
    id: Long = 1L,
    title: String = "테스트 그룹",
    category: GroupCategory = GroupCategory.STUDY,
    leaderMemberId: Long? = 1L,
    authorMemberId: Long = leaderMemberId ?: 1L,
    status: GroupStatus = GroupStatus.PROGRESS,
    visibility: GroupVisibility = GroupVisibility.MEMBER_ONLY,
    allowJoin: Boolean = true,
    memberCount: Long = 0,
    isPortfolioPublished: Boolean = false,
    createdAt: Instant = Instant.parse("2025-01-01T00:00:00Z"),
): Group {
    val group =
        Group(
            title = title,
            description = null,
            category = category,
            interests = null,
            techStack = null,
            repositoryUrls = null,
            allowJoin = allowJoin,
            visibility = visibility,
            authorMemberId = authorMemberId,
            createdAt = createdAt,
        )
    setField(group, "id", id)
    group.changeLeader(leaderMemberId)
    when (status) {
        GroupStatus.PROGRESS -> Unit
        GroupStatus.STOP -> group.stop()
        GroupStatus.SUCCESS, GroupStatus.FAIL -> group.end(status)
    }
    repeat(memberCount.toInt()) { group.increaseMemberCount() }
    if (isPortfolioPublished) group.publishPortfolio()
    return group
}

fun testGroupMember(
    groupId: Long,
    memberId: Long,
    joinedAt: Instant = Instant.parse("2025-01-01T00:00:00Z"),
): GroupMember = GroupMember(groupId = groupId, memberId = memberId, joinedAt = joinedAt)

// 생성을 완료해 Discord 채널이 연결된 그룹 Discord 채널을 만든다.
fun testGroupDiscordChannel(
    id: String = "channel-row-id",
    groupId: Long = 1L,
    discordChannelId: String = "discord-channel-id",
    createdAt: Instant = Instant.parse("2025-01-01T00:00:00Z"),
): GroupDiscordChannel =
    GroupDiscordChannel(id = id, groupId = groupId, createdAt = createdAt).apply {
        attachChannel(discordChannelId)
        complete(createdAt)
    }

fun setField(
    target: Any,
    fieldName: String,
    value: Any?,
) {
    val field = target.javaClass.getDeclaredField(fieldName)
    field.isAccessible = true
    field.set(target, value)
}
