# 그룹 상세 — 새로 필요한 API

그룹 API 설계 문서(`docs/plans/group/`) 중 현재 작업 트리의 스프링 구현에 아직 없는 API 목록입니다. 기존 API의 경로·계약을 변경하는 작업은 [api-changes.md](api-changes.md)에 있으며, 이 목록의 신규 구현과 구분합니다.

## 그룹 정보

| API | 문서 |
| :-- | :-- |
| `GET /groups/:groupId/info` | [그룹 정보 조회](../../plans/group/info/group-info-get.md) |

## 칸반 보드

| API | 문서 |
| :-- | :-- |
| `GET /groups/:groupId/lists` | [그룹 리스트 목록 조회](../../plans/group/kanban/group-lists-get.md) |
| `POST /groups/:groupId/lists` | [그룹 리스트 생성](../../plans/group/kanban/group-list-create.md) |
| `PATCH /groups/:groupId/lists/:listId` | [그룹 리스트 수정](../../plans/group/kanban/group-list-update.md) |
| `DELETE /groups/:groupId/lists/:listId` | [그룹 리스트 삭제](../../plans/group/kanban/group-list-delete.md) |
| `PATCH /groups/:groupId/lists/:listId/position` | [그룹 리스트 순서 변경](../../plans/group/kanban/group-list-position-update.md) |
| `GET /groups/:groupId/lists/:listId/cards` | [리스트 카드 목록 조회](../../plans/group/kanban/group-cards-get.md) |
| `POST /groups/:groupId/lists/:listId/cards` | [그룹 카드 생성](../../plans/group/kanban/group-card-create.md) |
| `PATCH /groups/:groupId/cards/:cardId/position` | [그룹 카드 위치 변경](../../plans/group/kanban/group-card-position-update.md) |

## 카드창

| API | 문서 |
| :-- | :-- |
| `GET /groups/:groupId/cards/:cardId` | [카드 단건 조회](../../plans/group/card-window/group-card-get.md) |
| `PATCH /groups/:groupId/cards/:cardId` | [그룹 카드 수정](../../plans/group/card-window/group-card-update.md) |
| `DELETE /groups/:groupId/cards/:cardId` | [그룹 카드 삭제](../../plans/group/card-window/group-card-delete.md) |
| `PUT /groups/:groupId/cards/:cardId/portfolio` | [카드 포트폴리오 포함 변경](../../plans/group/card-window/group-card-portfolio-update.md) |
| `PUT /groups/:groupId/cards/:cardId/labels` | [카드 레이블 변경](../../plans/group/card-window/group-card-labels-update.md) |
| `PUT /groups/:groupId/cards/:cardId/assignee` | [카드 담당자 변경](../../plans/group/card-window/group-card-assignee-update.md) |
| `PUT /groups/:groupId/cards/:cardId/disabled` | [카드 활성 상태 변경](../../plans/group/card-window/group-card-disabled-update.md) |
| `POST /groups/:groupId/cards/:cardId/cover-image/upload-link` | [카드 커버 이미지 업로드 링크 발급](../../plans/group/card-window/group-card-cover-image-upload-link.md) |
| `POST /groups/:groupId/cards/:cardId/cover-image` | [카드 커버 이미지 지정](../../plans/group/card-window/group-card-cover-image-update.md) |
| `DELETE /groups/:groupId/cards/:cardId/cover-image` | [카드 커버 이미지 제거](../../plans/group/card-window/group-card-cover-image-delete.md) |
| `GET /groups/:groupId/cards/:cardId/records` | [카드 기록 목록 조회](../../plans/group/card-window/group-card-records-get.md) |
| `DELETE /groups/:groupId/records/:recordId` | [카드 기록 삭제](../../plans/group/card-window/group-record-delete.md) |
| `POST /groups/:groupId/records/:recordId/restore` | [기록 복구](../../plans/group/card-window/group-record-restore.md) |
| `PUT /groups/:groupId/records/:recordId/portfolio` | [기록 포트폴리오 포함 변경](../../plans/group/card-window/group-record-portfolio-update.md) |
| `PUT /groups/:groupId/records/:recordId/archive` | [기록 개인 보관 변경](../../plans/group/card-window/group-record-archive.md) |
| `PUT /groups/:groupId/records/:recordId/position` | [기록 소속 카드 변경](../../plans/group/card-window/group-record-move.md) |

## 기록 편집창

| API | 문서 |
| :-- | :-- |
| `GET /groups/:groupId/records/:recordId` | [그룹 기록 단건 조회](../../plans/group/edit-window/group-record-get.md) |
| `GET /groups/:groupId/records/draft` | [기록 임시저장 본문 조회](../../plans/group/edit-window/group-record-draft-get.md) |
| `POST /groups/:groupId/cards/:cardId/records` | [카드 기록 작성](../../plans/group/edit-window/group-record-create.md) |
| `PATCH /groups/:groupId/records/:recordId` | [카드 기록 수정](../../plans/group/edit-window/group-record-update.md) |
| `POST /groups/:groupId/records/files/uploaded` | [기록 첨부 파일 업로드 완료](../../plans/group/edit-window/group-record-files-uploaded.md) |
| `PUT /groups/:groupId/records/draft` | [기록 임시저장 저장](../../plans/group/edit-window/group-record-draft-save.md) |
| `DELETE /groups/:groupId/records/draft` | [기록 임시저장 삭제](../../plans/group/edit-window/group-record-draft-delete.md) |

## 그룹 채팅

| API | 문서 |
| :-- | :-- |
| `GET /groups/:groupId/chat-messages` | [그룹 채팅 메시지 목록 조회](../../plans/group/chat/group-chat-messages-get.md) |
| `POST /groups/:groupId/chat-messages` | [그룹 채팅 메시지 전송](../../plans/group/chat/group-chat-message-send.md) |

## 저장소

| API | 문서 |
| :-- | :-- |
| `GET /groups/:groupId/repository/branches` | [그룹 저장소 브랜치 목록 조회](../../plans/group/repo/group-repository-branches-get.md) |
| `GET /groups/:groupId/repository/commits` | [그룹 저장소 커밋 목록 조회](../../plans/group/repo/group-repository-commits-get.md) |
| `GET /groups/:groupId/repository/file-tree` | [그룹 저장소 파일 트리 조회](../../plans/group/repo/group-repository-file-tree-get.md) |

## 검색

| API | 문서 |
| :-- | :-- |
| `GET /groups/:groupId/search` | [그룹 내 검색](../../plans/group/search/group-search.md) |

## 삭제된 카드·보관 기록·임시저장

| API | 문서 |
| :-- | :-- |
| `GET /groups/:groupId/deleted-cards` | [삭제된 카드 목록 조회](../../plans/group/archive/deleted-cards-get.md) |
| `POST /groups/:groupId/deleted-cards/:cardId/restore` | [삭제된 카드 복구](../../plans/group/archive/deleted-card-restore.md) |
| `GET /groups/:groupId/saved-records` | [보관한 기록 목록 조회](../../plans/group/archive/saved-records-get.md) |
| `GET /groups/:groupId/records/draft/meta` | [기록 임시저장 정보 조회](../../plans/group/edit-window/group-record-draft-metadata-get.md) |

## 그룹 관리·회원 설정

| API | 문서 |
| :-- | :-- |
| `PUT /groups/:groupId` | [그룹 정보 변경](../../plans/group/setting/group-update.md) |
| `PUT /groups/:groupId/status/end` | [그룹 활동 종료](../../plans/group/setting/group-status-end.md) |
| `PUT /groups/:groupId/status/stop` | [그룹 활동 중단](../../plans/group/setting/group-status-stop.md) |
| `PUT /groups/:groupId/status/progress` | [그룹 활동 재진행](../../plans/group/setting/group-status-progress.md) |
| `PATCH /groups/user-preference` | [GitHub API 개인 사용량 사용 설정](../../plans/group/setting/user-github-preference-update.md) |

## 설계 문서가 아직 없는 API

| 대상 | 제공할 데이터 |
| :-- | :-- |
| 접수 중인 세미나 조회 | 활동보고를 제출할 수 있는 세미나와 접수 기간 |
| GitHub API 개인 사용량 설정 조회 | 요청자에게 저장된 개인 사용량 설정 |
