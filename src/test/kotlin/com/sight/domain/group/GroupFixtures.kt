package com.sight.domain.group

import com.sight.core.groupauth.GroupVisibility
import java.time.Instant

// 지정한 식별자와 상태로 그룹 도메인을 만든다.
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
            id = id,
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
): GroupMember = GroupMember(id = memberId, groupId = groupId, memberId = memberId, joinedAt = joinedAt)

// 지정한 식별자로 그룹 Discord 채널을 만든다.
fun testGroupDiscordChannel(
    id: String = "channel-row-id",
    groupId: Long = 1L,
    discordChannelId: String = "discord-channel-id",
): GroupDiscordChannel = GroupDiscordChannel(id = id, groupId = groupId, discordChannelId = discordChannelId)

fun setField(
    target: Any,
    fieldName: String,
    value: Any?,
) {
    val field = target.javaClass.getDeclaredField(fieldName)
    field.isAccessible = true
    field.set(target, value)
}
