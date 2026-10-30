# 활동보고 파일 업로드 링크 발급

활동보고 파일을 올릴 업로드 주소(Presigned URL)를 발급합니다. 발급한 `url`로 파일 업로드를 완료한 뒤 `fileUploadId`를 활동보고 제출·수정 요청에 포함해야 합니다.

## 운영 그룹인 경우

(해당 없음)

## API 경로

```
POST /groups/:groupId/activity-reports/upload-link
```

## 권한

USER and 그룹장

## 조건

- 그룹이 종료(`SUCCESS`·`FAIL`) 상태가 아니어야 함. 중단 상태에서는 발급하며 그룹 상태는 바꾸지 않음. [업로드 링크 발급의 중단 상태 예외](../global.md#중단-상태에서의-동작-제한)를 따름

## 제한

- 업로드 파일 최대 크기: [업로드 링크 기준](../global.md#업로드-링크-기준)을 따름 (Presigned URL 생성 시 `content-length-range` 조건으로 R2 단에서 강제)

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

| 이름             | 타입   | 설명                              |
| ---------------- | ------ | --------------------------------- |
| `url`            | string | R2 Presigned URL. 유효 시간은 [업로드 링크 기준](../global.md#업로드-링크-기준)을 따름 |
| `fileKey`            | string | R2 object key                     |
| `fileUploadId` | 문자열 | `file_upload.id`               |


## 설계 결정: 공용 API 대신 개별 API로 분리한 이유

공용 Presigned URL API(`POST /files/presigned-url`)로 만들면 인증된 사용자라면 누구나 어떤 용도로든 key를 발급받을 수 있어 아래 두 가지 문제가 생긴다:

1. **key 용도 검증 불가**: 활동보고 제출 시 `reportFileKey`가 실제로 활동보고용으로 발급된 것인지 확인할 수 없다. 다른 API에서 발급받은 key를 가져다 쓰는 것을 막을 방법이 없다.

2. **권한 분리 불가**: 활동보고 업로드는 그룹장만 가능한데, 공용 API에 이 권한을 적용하면 다른 용도의 업로드도 함께 제한된다.

개별 API로 분리하면 `api_path` 컬럼에 발급 경로를 저장해두고, 활동보고 제출·수정 시 해당 key가 이 API에서 발급된 것인지 검증할 수 있다. R2 업로드 내부 함수는 재사용하되 API 진입점만 분리하는 방식이다.

## 테스트 케이스

1. 그룹장이 요청 → presigned URL + fileKey + fileUploadId 반환, `file_upload`에 `{ key, memberId, api, isUploaded=false }` INSERT, 200
2. 그룹원(비그룹장) 요청 → 403
3. 존재하지 않는 `groupId` 요청 → 404

## 관련 DB 테이블

| 테이블           | 역할                                                                                |
| ---------------- | ----------------------------------------------------------------------------------- |
| `group`          | 그룹 존재 확인 + `master` 컬럼으로 그룹장 여부 확인                                 |
| `file_upload` | 발급된 key INSERT (`id`, `key`, `memberId`, `api_path`, `isUploaded`, `created_at`) |
