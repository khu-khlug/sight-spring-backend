# 그룹 상세 조회

그룹 ID로 그룹의 상세 정보를 조회합니다. 공개 범위(grade) 기반의 열람 권한을 검증합니다.

## API

```
GET /groups/{groupId}
```

### 쿼리 파라미터

(해당 없음)

### 요청 바디

(해당 없음)

### 응답 코드 및 응답 바디

```
200 OK
```

| 이름                 |  타입  | 설명                                                                                  |
| :------------------- | :----: | :------------------------------------------------------------------------------------ |
| `id`                 |  숫자  | 그룹 ID                                                                               |
| `category`           | 문자열 | 그룹 카테고리 (`study`, `project`, `documentation`, `education`, `program`, `manage`) |
| `title`              | 문자열 | 그룹 이름                                                                             |
| `purpose`            | 문자열 | 그룹 목적 (nullable)                                                                  |
| `state`              | 문자열 | 그룹 상태 (`progress`, `suspend`, `end-success`, `end-fail`)                          |
| `interest`           | 문자열 | 관심 분야 (nullable)                                                                  |
| `technology`         | 문자열 | 기술 스택 (nullable)                                                                  |
| `allowJoin`          | 불리언 | 참여 허용 여부                                                                        |
| `grade`              |  숫자  | 공개 범위 (0=비공개, 2=운영진, 3=회원, 4=완전공개)                                    |
| `repository`         | 문자열 | 저장소 URL (nullable)                                                                 |
| `portfolio`          | 불리언 | 포트폴리오 발행 여부                                                                  |
| `countMember`        |  숫자  | 멤버 수                                                                               |
| `countList`          |  숫자  | 리스트 수                                                                             |
| `countCard`          |  숫자  | 카드 수                                                                               |
| `countRecord`        |  숫자  | 기록 수                                                                               |
| `createdAt`          | 문자열 | 생성일시 (ISO 8601)                                                                   |
| `changedAt`          | 문자열 | 마지막 활동일시 (ISO 8601)                                                            |
| `leader`                          |  객체  | 그룹장 정보                                       |
| `leader.userId`                   |  숫자  | 그룹장 회원 ID                                    |
| `leader.name`                     | 문자열 | 그룹장 이름                                       |
| `discordChannel`                  |  객체  | Discord 채널 정보 (nullable, 채널 미연동 시 null) |
| `discordChannel.id`               | 문자열 | Discord 채널 연동 ID                              |
| `discordChannel.discordChannelId` | 문자열 | Discord 채널 ID                                   |
| `discordChannel.createdAt`        | 문자열 | 채널 연동일시 (ISO 8601)                          |

### 테스트 케이스

1. 그룹 상세 정보를 정상적으로 조회할 수 있다
   - 열람 권한이 있는 그룹 ID를 전달하면 200 응답과 함께 그룹 상세 정보를 반환한다
2. 존재하지 않는 그룹은 조회할 수 없다
   - 존재하지 않는 그룹 ID를 전달하면 404 에러를 반환한다
3. 비공개(grade=0) 그룹은 그룹 멤버만 조회할 수 있다
   - 비공개 그룹에 멤버가 아닌 회원이 조회하면 403 에러를 반환한다
4. 운영진 공개(grade=2) 그룹은 운영진만 조회할 수 있다
   - 운영진 공개 그룹에 운영진이 아닌 회원이 조회하면 403 에러를 반환한다
5. 회원 공개(grade=3) 그룹은 KHLUG 회원(재학·휴학·졸업)만 조회할 수 있다
   - 교류 회원(state=-1)이 회원 공개 그룹을 조회하면 403 에러를 반환한다
6. 완전 공개(grade=4) 그룹은 교류 회원을 포함한 모든 회원이 조회할 수 있다
   - 교류 회원이 완전 공개 그룹을 조회하면 200 응답을 반환한다
7. Discord 채널이 연동된 그룹은 채널 정보를 함께 반환한다
   - Discord 채널이 연동된 그룹을 조회하면 `discordChannel` 필드에 채널 정보가 포함된다
8. Discord 채널이 미연동된 그룹은 `discordChannel`이 null로 반환된다
   - Discord 채널이 없는 그룹을 조회하면 `discordChannel` 필드가 null이다
