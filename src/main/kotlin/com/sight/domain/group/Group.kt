package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "khlug_group")
data class Group(
    @Id
    @Column(name = "id", nullable = false)
    val id: Long,

    @Column(name = "category", nullable = false, length = 255)
    @Convert(converter = GroupCategoryConverter::class)
    val category: GroupCategory,

    @Column(name = "title", nullable = false, length = 255)
    val title: String,

    @Column(name = "author", nullable = false)
    val author: Long,

    @Column(name = "master", nullable = false)
    val master: Long,

    @Column(name = "purpose", columnDefinition = "LONGTEXT")
    val purpose: String? = null,

    @Column(name = "state", nullable = false, length = 255)
    @ColumnDefault("'pending'")
    @Convert(converter = GroupStateConverter::class)
    val state: GroupState = GroupState.PENDING,

    @Column(name = "interest", length = 1023)
    val interest: String? = null,

    @Column(name = "technology", length = 255)
    val technology: String? = null,

    @Column(name = "allow_join", nullable = false, columnDefinition = "TINYINT")
    @ColumnDefault("0")
    val allowJoin: Boolean = false,

    @Column(name = "grade", nullable = false, columnDefinition = "TINYINT")
    @ColumnDefault("3")
    @Convert(converter = GroupAccessGradeConverter::class)
    val grade: GroupAccessGrade = GroupAccessGrade.MEMBER,

    @Column(name = "count_member", nullable = false, columnDefinition = "BIGINT")
    @ColumnDefault("0")
    val countMember: Long = 0,

    @Column(name = "count_list", nullable = false, columnDefinition = "BIGINT")
    @ColumnDefault("0")
    val countList: Long = 0,

    @Column(name = "count_card", nullable = false, columnDefinition = "BIGINT")
    @ColumnDefault("0")
    val countCard: Long = 0,

    @Column(name = "count_record", nullable = false, columnDefinition = "BIGINT")
    @ColumnDefault("0")
    val countRecord: Long = 0,

    @Column(name = "last_updater", columnDefinition = "BIGINT")
    val lastUpdater: Long? = null,

    @Column(name = "repository", length = 255)
    val repository: String? = null,

    @Column(name = "portfolio", nullable = false, columnDefinition = "TINYINT")
    @ColumnDefault("0")
    val portfolio: Boolean = false,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    // 레거시에서 nullable하게 정의되어 있으나 레거시에서 그룹 생성 시 값을 채워넣고 있기 때문에,
    // 컬럼 정의는 nullable하게 하나 타입 자체는 not nullable하게 정의합니다.
    @Column(name = "changed_at", nullable = true)
    val changedAt: LocalDateTime = LocalDateTime.now(),
)
