package com.sight.domain.file

// 업로드 확인 기한이 지났고 객체가 없는 파일의 행 잠금을 보유한 트랜잭션에서 동기 처리한다.
data class FileUploadMissingEvent(val fileUploadId: String)
