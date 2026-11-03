package com.sight.domain.group

enum class GroupDiscordChannelStatus {
    // 생성 작업을 기록했지만 채널 생성과 그룹장 참여가 끝나지 않은 상태. 재요청은 이 작업을 이어서 완료한다.
    CREATING,

    // 채널 생성과 그룹장 참여까지 끝난 상태.
    CREATED,
}
