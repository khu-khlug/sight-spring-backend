# 도서 정보 수정 API

이미 등록된 도서의 메타정보(제목/저자/출판사/발행연도/표지 이미지/설명/카테고리)를 운영진이
직접 수정하기 위한 API입니다. 대출 가능 권수(`totalCount`/`availableCount`)나 ISBN은 이 API로
바꿀 수 없습니다 — ISBN은 도서를 식별하는 불변 값이고, 권수는 등록(`POST /book/register`)/삭제
(`DELETE /book/{bookId}`)로만 바뀝니다.

권한: MANAGER

## API

```
PUT /book/{bookId}
```

### 경로 파라미터

| 이름     |  타입  | 설명           |
| :------- | :----: | :------------- |
| `bookId` | 문자열 | 도서 ID (ULID) |

### 요청 바디

| 이름            |  타입  | 필수 | 설명                                    |
| :-------------- | :----: | :--: | :-------------------------------------- |
| `title`         | 문자열 |  Y   | 제목, 빈 문자열 불가                    |
| `author`        | 문자열 |  Y   | 저자, 빈 문자열 불가                    |
| `publisher`     | 문자열 |  Y   | 출판사, 빈 문자열 불가                  |
| `publishedYear` |  정수  |  Y   | 발행연도, 양수여야 함                   |
| `coverImageUrl` | 문자열 |  N   | 표지 이미지 URL                         |
| `description`   | 문자열 |  N   | 설명                                    |
| `category`      |  enum  |  Y   | `BookCategory` 중 하나(아래 값 목록 참고) |

`category` 값: `LANGUAGE_FRAMEWORK`, `LIBERAL_ARTS`, `PRACTICAL`, `APP`,
`SOFTWARE_ENGINEERING`, `COMPUTER_SCIENCE`, `AI_DATA_SCIENCE`, `SECURITY_HACKING`, `MATH`,
`WEB_NETWORK`, `HARDWARE_SYSTEM_PROGRAMMING`, `OTHER`

### 응답 코드 및 응답 바디

```
204 No Content
```

응답 본문 없음. 변경된 정보를 확인하려면 별도로 `GET /book/{bookId}`를 다시 호출해야 합니다.

### 테스트 케이스

1. 운영진이 존재하는 `bookId`로 유효한 요청 → 204, 도서 정보가 요청 값으로 갱신됨
2. 존재하지 않는 `bookId`로 요청 → 404
3. `title`/`author`/`publisher`가 빈 문자열 → 400
4. `publishedYear`가 0 이하 → 400
5. `category`가 없거나 `BookCategory`에 없는 값 → 400
6. MANAGER가 아닌 회원이 요청 → 403
7. 미인증 요청 → 401
