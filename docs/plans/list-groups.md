# 그룹 목록 조회

그룹 목록을 다양한 조건으로 필터링하여 조회합니다. 카테고리별(study, project, manage 등), 상태별(진행 중/성공), 관심 분야별 필터와 키워드 검색을 지원하며, 기존의 참여 여부·즐겨찾기 필터와 조합하여 사용할 수 있습니다.

## API

```
GET /groups
```

### 쿼리 파라미터

| 이름         |  타입  | 설명                                                                                  |
| :----------- | :----: | :------------------------------------------------------------------------------------ |
| `offset`     |  숫자  | 조회 시작 위치 (기본값: 0)                                                            |
| `limit`      |  숫자  | 조회 개수 (기본값: 10, 최대: 100)                                                     |
| `joined`     | 불리언 | `true`이면 내가 참여 중인 그룹만 조회                                                 |
| `bookmarked` | 불리언 | `true`이면 내가 즐겨찾기한 그룹만 조회                                                |
| `category`   | 문자열 배열 | 그룹 카테고리 필터. `study`, `project`, `manage`, `documentation`, `program`, `education` 중 복수 선택 가능. 예: `?category=study&category=project` |
| `state`      | 문자열 | 그룹 상태 필터. `progress` (진행 중) 또는 `end-success` (종료-성공)                    |
| `interest`   | 문자열 | 관심 분야 키워드 (예: `웹`, `AI`). `members_interest.name`에 등록된 값                |
| `keyword`    | 문자열 | 검색 키워드. 제목(`title`), 목적(`purpose`), 기술 스택(`technology`)에서 부분 일치 검색 |
| `orderBy`    | 문자열 | 정렬 기준. `changedAt` (최근 활동순) 또는 `createdAt` (생성순, 기본값). 정렬 방향은 내림차순(DESC) 고정 |

### 요청 바디

(해당 없음)

### 응답 코드 및 응답 바디

```
200 OK
```

| 이름                   |   타입    | 설명                         |
| :--------------------- | :-------: | :--------------------------- |
| `count`                |   숫자    | 필터 조건에 맞는 전체 그룹 수 |
| `groups`               | 객체 배열 | 그룹 목록                    |
| `groups.id`            |   숫자    | 그룹 ID                     |
| `groups.category`      |  문자열   | 그룹 카테고리                |
| `groups.title`         |  문자열   | 그룹 이름                    |
| `groups.state`         |  문자열   | 그룹 상태                    |
| `groups.countMember`   |   숫자    | 멤버 수                      |
| `groups.allowJoin`     |  불리언   | 참여 허용 여부               |
| `groups.createdAt`     |  문자열   | 생성 일시 (ISO 8601)         |
| `groups.leader`        |   객체    | 그룹장 정보                  |
| `groups.leader.userId` |   숫자    | 그룹장 회원 ID               |
| `groups.leader.name`   |  문자열   | 그룹장 이름                  |

### 테스트 케이스

1. `category=study`로 조회하면 스터디 카테고리 그룹만 반환한다
2. `category=study&category=project`로 조회하면 스터디, 프로젝트 카테고리 그룹만 반환한다
   - 내부적으로 `WHERE category IN ('study', 'project')` 형태로 검색한다
3. `category` 파라미터에 유효하지 않은 값을 전달하면 400 에러를 반환한다
4. `state=progress`로 조회하면 진행 중 그룹만 반환한다
5. `state=end-success`로 조회하면 성공 종료 그룹만 반환한다
6. `state` 파라미터에 유효하지 않은 값을 전달하면 400 에러를 반환한다
7. `interest=웹`으로 조회하면 관심 분야에 "웹"이 포함된 그룹만 반환한다
   - `group.interest` 컬럼에서 `|`로 구분된 값 중 정확히 일치하는 그룹만 포함한다
8. 관심 분야가 설정되지 않은 그룹(`interest`가 NULL)은 결과에 포함되지 않는다
9. 제목에 키워드가 포함된 그룹을 검색할 수 있다
   - `keyword=Spring`으로 검색하면 `title`에 "Spring"이 포함된 그룹이 결과에 포함되어야 한다
10. 목적에 키워드가 포함된 그룹을 검색할 수 있다
    - `keyword=학습`으로 검색하면 `purpose`에 "학습"이 포함된 그룹이 결과에 포함되어야 한다
11. 기술 스택에 키워드가 포함된 그룹을 검색할 수 있다
    - `keyword=Kotlin`으로 검색하면 `technology`에 "Kotlin"이 포함된 그룹이 결과에 포함되어야 한다
12. 키워드는 제목, 목적, 기술 스택 중 하나라도 일치하면 결과에 포함된다 (OR 조건)
13. `keyword`가 빈 문자열이면 필터를 적용하지 않는다
14. 모든 필터를 지정하지 않으면 전체 그룹을 반환한다
15. 여러 필터를 동시에 사용할 수 있다 (AND 조합)
    - `category=study&category=project&state=progress&keyword=Spring` 같은 조합이 정상 동작해야 한다
16. `count`는 페이지네이션과 무관하게 필터 조건에 맞는 전체 그룹 수를 반환한다
