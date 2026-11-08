package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

// 회원의 그룹 Discord 채널 참여. Discord 호출 전에 참여 작업으로 먼저 저장해, Discord 참여 후 완료 기록이 실패해도 재요청에서 이어서 완료한다.
@Entity
// 유니크(discord_channel_id, member_id): 회원·채널별 참여 작업은 하나.
@Table(
    name = "khlug_group_discord_channel_member",
    uniqueConstraints = [UniqueConstraint(columnNames = ["discord_channel_id", "member_id"])],
)
class GroupDiscordChannelMember(
    id: Long,
    discordChannelId: String,
    memberId: Long,
    createdAt: Instant,
) {
    @Id
    @Column(name = "id", nullable = false)
    val id: Long = id

    // 참여할 그룹 Discord 채널 행(GroupDiscordChannel.id). 회원과 함께 유일해 회원·채널별 참여를 하나로 직렬화한다.
    @Column(name = "discord_channel_id", nullable = false, length = GroupDiscordChannel.ID_MAX_LENGTH)
    val discordChannelId: String = discordChannelId

    // 참여하는 회원. 항상 요청자 본인이다.
    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    // 참여 작업을 기록한 일시.
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = createdAt

    // 참여를 완료한 일시. null이면 참여 작업이 진행 중이다.
    @Column(name = "joined_at")
    var joinedAt: Instant? = null
        private set

    init {
        require(id > 0) { "식별자가 올바르지 않습니다." }
        require(discordChannelId.isNotBlank()) { "Discord 채널 식별자가 올바르지 않습니다." }
        require(memberId > 0) { "회원 식별자가 올바르지 않습니다." }
    }

    val isJoined: Boolean
        get() = joinedAt != null

    fun completeJoin(at: Instant) {
        require(!isJoined) { "이미 Discord 채널에 참여했습니다." }
        joinedAt = at
    }
}
