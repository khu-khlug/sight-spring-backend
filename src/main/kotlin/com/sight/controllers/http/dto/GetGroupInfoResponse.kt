package com.sight.controllers.http.dto

import java.time.Instant

data class GetGroupInfoResponse(
    val id: String,
    val title: String,
    val description: String?,
    val category: String,
    val status: String,
    val requesterInfo: GroupInfoRequesterResponse,
    val specialOrganization: String?,
    val interests: List<String>?,
    val techStack: List<String>?,
    val repositoryUrls: List<String>?,
    val memberCount: Long,
    val activity: GroupInfoActivityResponse,
    val createdAt: Instant,
    val lastActivityAt: Instant,
    val allowJoin: Boolean,
    val visibility: String,
    val exposure: GroupInfoExposureResponse,
    val hasChatRoom: Boolean,
    val chatRoomUrl: String?,
)

data class GroupInfoRequesterResponse(
    val userRole: String,
    val isBookmarked: Boolean,
    val isInChatRoom: Boolean?,
)

data class GroupInfoActivityResponse(
    val listCount: Long,
    val cardCount: Long,
    val recordCount: Long,
)

data class GroupInfoExposureResponse(
    val isPortfolioPublished: Boolean,
    val portfolioListCount: Long,
    val portfolioCardCount: Long,
    val seminarCount: Long,
    val reportCount: Long,
)
