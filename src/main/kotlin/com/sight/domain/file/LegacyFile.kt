package com.sight.domain.file

import java.time.Instant

// khlug_files의 기존 행. 원본 테이블에는 PK·유일 제약이 없어 JPA 식별자를 임의로 부여하지 않는다.
// 그룹 외 파일도 포함하며 신규 업로드의 완료 확인·청소 대상이 아니다.
// 캐시를 저장할 때 기존 updated_at의 자동 갱신을 억제하고 원래 값을 보존해야 한다.
class LegacyFile(
    val article: Long,
    val author: Long,
    val original: String,
    val name: String,
    val extension: String,
    val mime: String,
    val size: Long,
    val type: String,
    val countDownload: Long,
    val updatedAt: Instant,
    val createdAt: Instant,
    val readCache: FileReadCache = FileReadCache(),
)
