package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

// 그룹 채팅방인 Discord 채널. 그룹당 하나이며, Discord 호출 전에 생성 작업으로 먼저 저장해 중간 실패를 복구한다.
@Entity
// 유니크(group_id): 그룹당 채널 하나, 동시 생성 방지.
@Table(
    name = "khlug_group_discord_channel",
    uniqueConstraints = [UniqueConstraint(columnNames = ["group_id"])],
)
class GroupDiscordChannel(
    id: String,
    groupId: Long,
    createdAt: Instant,
) {
    // 채널 행 식별자이자 생성 작업 식별자. Discord에 만든 채널을 이 식별자로 다시 찾는다. 서버를 재시작해도 남는다.
    @Id
    @Column(name = "id", nullable = false, length = ID_MAX_LENGTH)
    val id: String = id

    // 채널을 가진 그룹. 유일해 동시 요청에도 채널을 하나만 만든다.
    @Column(name = "group_id", nullable = false)
    val groupId: Long = groupId

    // Discord 채널 아이디. 채널을 만들기 전이면 null이다.
    @Column(name = "discord_channel_id", length = 255)
    var discordChannelId: String? = null
        private set

    // 생성 진행 상태.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: GroupDiscordChannelStatus = GroupDiscordChannelStatus.CREATING
        private set

    // 생성 작업을 기록한 일시.
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = createdAt

    // 생성을 완료한 일시. 완료 전이면 null이다.
    @Column(name = "completed_at")
    var completedAt: Instant? = null
        private set

    init {
        require(id.isNotBlank() && id.length <= ID_MAX_LENGTH) { "Discord 채널 식별자가 올바르지 않습니다." }
        require(groupId > 0) { "그룹 식별자가 올바르지 않습니다." }
    }

    val isCreated: Boolean
        get() = status == GroupDiscordChannelStatus.CREATED

    // Discord에 만든 채널을 연결한다. 이미 연결된 채널과 다른 채널은 연결할 수 없다.
    fun attachChannel(discordChannelId: String) {
        require(discordChannelId.isNotBlank()) { "Discord 채널 아이디가 올바르지 않습니다." }
        require(this.discordChannelId == null || this.discordChannelId == discordChannelId) {
            "이미 다른 Discord 채널이 연결돼 있습니다."
        }
        this.discordChannelId = discordChannelId
    }

    // 채널 생성과 그룹장 참여가 끝났을 때 완료한다.
    fun complete(at: Instant) {
        require(discordChannelId != null) { "Discord 채널이 연결되지 않았습니다." }
        require(!isCreated) { "이미 생성을 완료한 Discord 채널입니다." }
        status = GroupDiscordChannelStatus.CREATED
        completedAt = at
    }

    companion object {
        const val ID_MAX_LENGTH: Int = 100
    }
}
