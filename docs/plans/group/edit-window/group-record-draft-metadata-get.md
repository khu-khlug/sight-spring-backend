# 기록 임시저장 정보 조회

요청자의 기록 임시저장 메타데이터(본문 제외)를 조회합니다. 임시저장은 회원당 하나뿐이며, 그 임시저장이 경로의 그룹 것일 때만 반환합니다. 임시저장이 없거나 다른 그룹 것이면 오류가 아니므로 404 대신 `draft`를 null로 반환합니다.

## 운영 그룹인 경우

(해당 없음)

## API 경로

```
GET /groups/:groupId/records/draft/meta
```

## 권한

USER and 그룹원

## 조건

(해당 없음)

## 부수 효과

(해당 없음)

## 쿼리스트링

(해당 없음)

## 요청 바디

(해당 없음)

## 성공 응답

```
200 OK
```

| 이름 | 타입 | 설명 |
| :-- | :--: | :-- |
| `draft` | 객체 \| null | 요청자의 기록 임시저장. 없거나 다른 그룹의 임시저장이면 null |
| `draft.isNewRecord` | 불리언 | 새 기록 작성 중이면 true, 기존 기록 수정 중이면 false |
| `draft.cardId` | 문자열 | 작성 중인 기록이 속한 카드 아이디 |
| `draft.cardTitle` | 문자열 | 카드명 |
| `draft.recordId` | 문자열 \| null | 수정 중인 기록 아이디. `isNewRecord`가 true이면 null |
| `draft.updatedAt` | 문자열 | 마지막 저장 일시 (ISO 8601 UTC, `Z` 접미사) |
