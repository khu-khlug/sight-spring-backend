# 관심분야 추가 요청 목록 조회 API
- 메소드: POST
- URL: /fields
- 사용자 권한: 운영진

## 쿼리 스트링

- (없음)

## 요청 바디

- 

```json
{
  "fieldName": "fieldName"
}
```

## 응답 코드 및 응답 바디

- 201

```json
{
  "fieldId":   "fieldId",
  "fieldName": "fieldName",
  "createdAt": "createdAt"
}
```

## 테스트 시나리오

### 운영진이 아닌 경우 에러 반환

- Given: 유저가 운영진이 아니다.
- When: 관심분야를 추가 시도한다.
- Then: 에러를 반환해야 한다.

### 관심분야 이름이 빈 문자열이면 에러 반환

- Given: 관심분야 이름이 빈 문자열이다.
- When: 관심분야를 추가 시도한다.
- Then: 에러를 반환해야 한다.

### 관심분야 이름이 공백 문자로만 이루어져 있으면 에러 반환

- Given: 관심분야 이름이 공백문자로만 이뤄져있다.
- When: 관심분야를 추가 시도한다.
- Then: 에러를 반환해야 한다.

### 관심분야 이름이 이미 존재하고 obsoletedAt이 null이면 에러 반환

- Given: 관심분야 이름이 이미 존재하고 obsoletedAt이 null이다.
- When: 관심분야를 추가 시도한다.
- Then: 에러를 반환해야 한다.

### 관심분야 이름이 이미 존재하고 obsoletedAt이 null이 아니면 관심분야를 재활성화(클라 입장에선 추가 성공)

- Given: 관심분야 이름이 이미 존재하고 obsoletedAt이 null이 아니다.
- When: 관심분야를 추가 시도한다.
- Then: obsoletedAt을 null로 변경하고 obsoleteReason을 null로 변경하고 createdAt을 처리 시간으로 재설정한 후 재활성화한 관심분야 정보를 반환한다.

### 적합한 유저가 적합한 요청을 하면 관심분야를 추가

- Given: 요청바디가 적합하다.
- When: 적합한 유저가 관심분야를 추가 시도한다.
- Then: 관심분야를 추가하고 생성된 관심분야 정보를 반환한다.