
* 페이지네이션이나 정렬같은건 DB에서처리하나요서비스에서처리하나요?

# 선택한 그룹 매칭 별 응답 목록 조회 API
- 메소드: GET
- URL: /group-matchings/:groupMatchingId/answers
- 사용자 권한: 운영진

## 구현 시 참고해야 하는 부분

- 쿼리 조건: `groupType`(스터디 / 프로젝트), `fieldId`
- 정렬 조건: 제출 생성 일시 기준 내림차순(최신이 위로)
- 페이지네이션 적용 필요
- 응답에는 해당 응답으로 생성된 그룹의 수도 같이 응답해야 함.
→ answers.matchedGroupIds.length
- 유효하지 않은 값에 대처하는 용도로 디폴트 값을 사용하면 안 됨 
→ 대신 명시적으로 에러 발생

## 쿼리 스트링

- `offset: uint = DEFAULT_OFFSET (optional)`
- `limit: uint = DEFAULT_LIMIT (optional)`
- `groupType: “STUDY” | “PROJECT” (optional)`
- `fieldId: string (optional) // 필터는 하나만 가능`

## 요청 바디

- (없음)

## 응답 코드 및 응답 바디

- 200

```
{
	  "answers":
	  {
	      "answerId":        "answerId",
			  "answerUserId":     123,
			  "createdAt":       "createdAt",
			  "updatedAt":       "updatedAt",
			  
			  "groupType":       "STUDY" | "PROJECT",
			  "isPreferOnline":   true | false,
			  "selectedFields":  ["fieldId"],
			  "subjectIdeas":    ["groupMatchingSubject"],
			  "matchedGroupIds": [123]
		}[],
	  "total":                123, // 전체 개수
}
```

## 테스트 시나리오

### 운영진이 아닌 경우 에러

- Given: 유저가 운영진이 아니다.
- When: 그룹매칭 응답 목록을 조회한다.
- Then: 에러를 반환해야 한다.

### 그룹매칭id가 존재하지 않으면 에러

- Given: 그룹매칭id가 존재하지 않는다.
- When: 적합한 유저가 그룹매칭 응답 목록을 조회한다.
- Then: 에러를 반환해야 한다.

### 해당 그룹매칭에 응답이 없으면 빈 배열 반환

- Given: 해당 그룹매칭에 응답 개수가 0이다.
- When: 적합한 유저가 그룹매칭 응답 목록을 조회한다.
- Then: answers는 빈배열, total은 0, hasNext는 false로 반환해야 한다.

### 목록은 생성 일시 기준 내림차순

- Given: 에러가 반환되지 않는 적합한 요청이다.
- When: 적합한 유저가 그룹매칭 응답 목록을 조회한다.
- Then: answers는 createdAt에 대해 내림차순으로 정렬되어야 한다.

### 선택한 관심분야가 없는 경우 에러

- Given: 응답에 선택한 관심분야가 존재하지 않는다.
- When: 적합한 유저가 그룹매칭 응답 목록을 조회한다.
- Then: 에러를 반환한다.

### 제시한 주제/아이디어가 없으면 빈배열

- Given: 응답에 제시한 주제/아이디어가 존재하지 않는다.
- When: 적합한 유저가 그룹매칭 응답 목록을 조회한다.
- Then: 해당 응답의 subjectIdeas는 빈 배열이어야 한다.

### 매칭된 그룹이 없으면 빈 배열

- Given: 응답에 매칭된 그룹이 존재하지 않는다.
- When: 적합한 유저가 그룹매칭 응답 목록을 조회한다.
- Then: 해당 응답의 matchedGroupIds는 빈 배열이어야 한다.

### groupType 값이 적합하지 않은 경우 에러

- Given: groupType 쿼리스트링 값이 "STUDY"나 "PROJECT"가 아니다.
- When: 적합한 유저가 그룹매칭 응답 목록을 조회한다.
- Then: 에러를 반환한다.

### groupType 값이 적합할 경우 필터링

- Given: groupType 쿼리스트링 값이 "STUDY"나 "PROJECT”이다.
- When: 적합한 유저가 그룹매칭 응답 목록을 조회한다.
- Then: 해당 groupType인 응답만을 응답목록에 포함시킨다.

### fieldId 값이 유효하지 않은 경우 에러

- Given: fieldId 쿼리스트링 값이 유효하지 않다.
- When: 적합한 유저가 그룹매칭 응답 목록을 조회한다.
- Then: 에러를 반환한다.

### fieldId 값이 유효할 경우 필터링

- Given: fieldId 쿼리스트링 값이 존재한다.
- When: 적합한 유저가 그룹매칭 응답 목록을 조회한다.
- Then: 해당 fieldId를 갖는 응답만을 응답목록에 포함시킨다.

### fieldId와 groupType 값이 모두 유효할 경우 필터링

- Given: fieldId와 groupType 값이 모두 유효하다.
- When: 적합한 유저가 그룹매칭 응답 목록을 조회한다.
- Then: 해당 fieldId를 갖고 해당 groupType인 응답만을 응답목록에 포함시킨다.

### offset이 음의 정수면 에러

- Given: offset이 음의 정수다.
- When: 적합한 유저가 그룹매칭 응답 목록을 조회한다.
- Then: 에러를 반환한다.

### offset이 total 보다 크면 빈 배열 반환

- Given: offset이 total 보다 크다.
- When: 적합한 유저가 그룹매칭 응답 목록을 조회한다.
- Then: 빈 배열을 반환한다.

### limit이 양의 정수가 아니면 에러

- Given: limit이 양의 정수가 아니다.
- When: 적합한 유저가 그룹매칭 응답 목록을 조회한다.
- Then: 에러를 반환한다.

### offset과 limit가 적합하면 해당 범위의 응답목록을 반환

- Given: offset과 limit가 적합하다.
- When: 적합한 유저가 그룹매칭 응답 목록을 조회한다.
- Then: 필터링과 내림차순이 적용된 목록의 [offset, min(total,offset+litmit)-1]을 인덱스 범위로 하는 응답목록을 반환한다.