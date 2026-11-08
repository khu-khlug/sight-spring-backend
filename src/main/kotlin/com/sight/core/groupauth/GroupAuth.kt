package com.sight.core.groupauth

/**
 * @param permissions 요청자가 충족해야 할 그룹 권한 목록. 하나라도 만족하면 허용한다.
 * @param groupId 그룹 ID를 읽을 경로 변수 이름.
 * @param recordId RECORD_AUTHOR 판정에 사용할 기록 ID의 경로 변수 이름.
 * @param allowManager 그룹 권한과 별개로 운영진을 허용할지 호출부에서 명시한다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GroupAuth(
    val permissions: Array<GroupPermission>,
    val groupId: String,
    val recordId: String = "",
    val allowManager: Boolean,
)
