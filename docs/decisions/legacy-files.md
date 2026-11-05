# 레거시 파일·이미지 제공

- 레거시 `khlug_files`는 사이트 전체 파일을 담으므로 기존 데이터와 컬럼을 유지해 Spring에서 사용하며 읽기 URL 캐시 컬럼만 추가한다. 신규 `file_upload`와 별도로 유지하며 R2 객체 키와 기존 기록의 HTML 원문도 유지한다.
- 파일 테이블의 분리·통합은 사이트 전체의 Spring 전환 이후 별도 작업으로 결정한다. 이번 전환에서는 레거시 파일을 신규 테이블로 옮기지 않는다.
- 기존 `/file/{filename}`·`/image/{filename}`은 Spring이 메타데이터와 접근 권한을 확인한 뒤 Presigned GET URL로 HTTP 302 리다이렉션한다.
- 그룹 파일은 `/groups/{groupId}/files/{fileId}`에서 그룹 열람 권한으로 제공한다. 더 강한 권한의 파일은 각 기능 API에서 별도로 제공한다. 추후 다른 Spring 서비스도 구체적인 파일 경로를 사용하며 `/file/{filename}`·`/image/{filename}`은 레거시용으로만 사용한다.
- 신규 기록은 본문에 영구 주소(`/groups/{groupId}/files/{fileId}`)를 저장한다. 프런트는 렌더링 시 Presigned URL을 주입하지 않으며 백엔드가 URL 발급·캐싱을 담당한다.
- 서버 운영자는 기존 `khlug.org/file/{filename}`, `khlug.org/image/{filename}`의 파일 요청도 Caddy에서 Spring으로 전달하고 Laravel 종료 후에도 기존 주소를 유지한다.
- 테이블 유지와 제공 전환은 [레거시 파일 제공 전환](../plans/file-migration.md), 신규 접근 계약은 [그룹 파일 조회](../plans/group/group-file-get.md)를 따른다.
