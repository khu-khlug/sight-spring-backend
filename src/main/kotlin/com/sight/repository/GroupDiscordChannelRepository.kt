package com.sight.repository

import com.sight.domain.group.GroupDiscordChannel
import org.springframework.data.jpa.repository.JpaRepository

interface GroupDiscordChannelRepository : JpaRepository<GroupDiscordChannel, String> {
    fun findByGroupId(groupId: Long): GroupDiscordChannel?

    fun existsByGroupId(groupId: Long): Boolean
}
