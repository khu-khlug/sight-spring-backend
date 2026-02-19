package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

data class GroupMemberId(
    val group: Long = 0,
    val member: Long = 0,
) : Serializable

@Entity
@Table(name = "khlug_group_member")
@IdClass(GroupMemberId::class)
data class GroupMember(
    @Id
    @Column(name = "`group`", nullable = false)
    val group: Long,

    @Id
    @Column(name = "member", nullable = false)
    val member: Long,
)
