package com.sight.domain.group

/**
 * 그룹 관련 정책 상수.
 */
object GroupPolicy {
    /**
     * 그룹 활용 실습 그룹의 ID.
     *
     * 경험치(ExPoint) 부여 대상이 아니므로 참여/내보내기/탈퇴 시 포인트 처리에서 제외한다.
     */
    const val EXPOINT_EXCLUDED_GROUP_ID = 7549L
}
