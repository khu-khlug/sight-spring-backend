package com.sight.domain.ideacloud

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "khlug_ideacloud")
data class IdeaCloud(
    @Id
    @Column(name = "id", nullable = false)
    val id: Long,

    @Column(name = "idea", nullable = false, length = 255)
    val idea: String,

    @Column(name = "author", nullable = false)
    val author: Long,

    @Column(name = "state", nullable = false, length = 255)
    @Convert(converter = IdeaCloudStateConverter::class)
    val state: IdeaCloudState = IdeaCloudState.PUBLIC,

    @Column(name = "created_at", nullable = true)
    val createdAt: LocalDateTime? = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = true)
    val updatedAt: LocalDateTime? = LocalDateTime.now(),
)
