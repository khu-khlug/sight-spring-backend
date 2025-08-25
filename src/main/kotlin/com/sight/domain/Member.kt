package com.sight.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "khlug_members")
data class Member(
    @Id
    val id: Long,

    @Column(name = "name", unique = true, nullable = false, length = 127)
    val name: String,

    @Column(name = "password", length = 255)
    val password: String? = null,

    @Column(name = "number")
    val number: Long? = null,

    @Column(name = "admission", nullable = false, columnDefinition = "CHAR(2)")
    val admission: String = "",

    @Column(name = "realname", nullable = false, length = 255)
    val realname: String = "",

    @Column(name = "college", nullable = false, length = 255)
    val college: String = "",

    @Column(name = "grade", nullable = false)
    val grade: Long = 0L,

    @Column(name = "state", nullable = false)
    val state: Long = -1L,

    @Column(name = "email", length = 255)
    val email: String? = null,

    @Column(name = "phone", length = 255)
    val phone: String? = null,

    @Column(name = "homepage", length = 255)
    val homepage: String? = null,

    @Column(name = "language", length = 255)
    val language: String? = null,

    @Column(name = "interest", length = 1023)
    val interest: String? = null,

    @Column(name = "prefer", length = 255)
    val prefer: String? = null,

    @Column(name = "expoint", nullable = false)
    val expoint: Long = 0L,

    @Column(name = "active", nullable = false, columnDefinition = "TINYINT")
    val active: Boolean = false,

    @Column(name = "manager", nullable = false, columnDefinition = "TINYINT")
    val manager: Boolean = false,

    @Column(name = "slack", length = 100)
    val slack: String? = null,

    @Column(name = "remember_token", length = 100)
    val rememberToken: String? = null,

    @Column(name = "khuisauth_at", nullable = false)
    val khuisauthAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "return_at")
    val returnAt: LocalDateTime? = null,

    @Column(name = "return_reason", length = 191)
    val returnReason: String? = null,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_login", nullable = false)
    val lastLogin: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_enter", nullable = false)
    val lastEnter: LocalDateTime = LocalDateTime.now(),
)
