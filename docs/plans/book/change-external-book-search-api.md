# 도서 정보 조회 - 네이버 API → 도서관정보나루 API 전환

## 배경

네이버 책 검색 API(`openapi.naver.com`)가 서비스 종료됐다. ISBN으로 도서 메타정보(제목/저자/출판사/출판연도/표지이미지/책소개)를 가져오는 용도로 `BookActionService.registerBook`(도서 등록)과 `BookService.previewBook`(등록 전 미리보기) 두 곳에서 쓰고 있다.

## 후보

규모가 크면서 무료인 도서 검색 API들을 조사한 결과이다.

| 항목 | 도서관정보나루 (data4library.kr) | 국립중앙도서관 서지정보 (SEOJI) | 카카오 도서 검색 API |
|---|---|---|---|
| 운영주체 | 국립중앙도서관 (문체부 산하) | 국립중앙도서관 | 카카오 |
| 종료 위험 | 낮음 (공공서비스) | 낮음 (공공서비스) | 중간 (민간, 정책변경 가능) |
| ISBN 검색 | 지원 | 지원 | 지원 (target=isbn) |
| 제목/저자/출판사 | 제공 | 제공 | 제공 |
| 표지 이미지 | 제공 | 미제공 | 제공 |
| 책소개 | 제공 | 미제공(정가·발행년 등 서지정보 위주) | 제공 (contents, 요약본) |
| 응답 형식 | XML/JSON | XML | JSON |
| 인증 방식 | authKey (무료 발급) | 인증키 (무료 발급) | REST API 키 (무료 발급) |
| 호출 제한 | 별도 명시된 큰 제한 없음(과다 트래픽 시 제재) | 기관용 제한 있음 | 일일/초당 제한 있음 |
| 데이터 범위 | 국내 공공도서관 소장 도서 중심 | 국내 발행 전체 도서(납본 기반) | 온라인서점 유통 도서 중심 |

## 선정

기존 네이버 API 대신 **도서관정보나루 API**를 사용한다.

### 선정의 이유
- 네이버와 알라딘이 차례로 도서 검색 서비스를 종료하면서 주요 기업들의 도서검색 API가 서비스를 종료하기 시작했다. 이러한 원인이 무엇인지 인사이트는 없지만 카카오도 서비스 종료 위험이 있다고 생각된다.
- 국립중앙도서관 API와 도서관정보나루 API 모두 국립중앙도서관에서 관리되지만 도서관정보나루 API는 표지 이미지와 도서 정보 등을 제공한다.
- 국내 출간 도서는 출판시 국립중앙도서관에 책이 등록되므로 최신의 책 정보도 제공된다.

## 방침: 기존 코드는 Obsolete로 남기고 새 코드만 추가

기존 API 엔드포인트와 응답 스키마는 그대로 유지한다. 바뀌는 건 서버 내부에서 도서 메타정보를 어디서 가져오는지뿐이다.

호출부(`BookActionService`, `BookService`)만 새 클라이언트를 쓰도록 바꾼다.

기존코드는 삭제하지 않고 `@Deprecated`로 표시만 해서 코드에 남겨둔다.
새로 추가한 코드가 정상동작됨이 프로덕션에서 확인되면 삭제한다.

## 새 API: 도서관정보나루 `srchDtlList`

```
GET https://data4library.kr/api/srchDtlList?authKey={authKey}&isbn13={isbn13}&format=json
```

- `authKey`, `isbn13`(13자리) 필수. `format=json`으로 JSON 응답이 오는 것까지는 확인함. 다만 발급한 키가 아직 활성화 전이라 실제 `detail.book` 페이로드는 못 봤음 — 활성화되는 대로 필드 재검증 필요.
- 응답 구조: `{ response: { request: {...}, resultNum, detail: [{ book: { bookname, authors, publisher, publication_year, isbn13, bookImageURL, description, ... } }] } }`

## 필드 매핑

| 정보나루 응답 필드 | 기존 `NaverBookItem` 필드 | 비고 |
| --- | --- | --- |
| `bookname` | `title` | |
| `authors` | `author` | ⚠️ "지은이: 김호연" 처럼 역할 접두어가 붙는 경우가 있다고 알려짐 — 실제 응답 확인 후 정제 로직 필요 여부 판단 |
| `publisher` | `publisher` | |
| `publication_year` | `publishedYear` | 기존 코드는 `pubdate.take(4).toIntOrNull()`로 연도만 뽑아 썼는데, 정보나루는 애초에 연도만 주므로 `take(4)` 로직 자체가 불필요해짐 |
| `bookImageURL` | `coverImageUrl`(구 `image`) | |
| `description` | `description` | |

## 변경 파일

- **신규**: `src/main/kotlin/com/sight/core/data4library/Data4LibraryBookClient.kt` — `NaverBookClient`와 같은 계층에 신설. `searchByIsbn(isbn): Data4LibraryBookItem?` 시그니처로 기존 `NaverBookClient.searchByIsbn`과 동일한 형태 유지(호출부 diff 최소화).
- **기존 유지(Obsolete 표시만)**: `src/main/kotlin/com/sight/core/naver/NaverBookClient.kt` — 클래스/함수에 `@Deprecated("네이버 서비스 종료. data4library API로 대체")` 추가.
- **수정**: `BookActionService.kt`, `BookService.kt` — 생성자 주입을 `naverBookClient: NaverBookClient` → `data4LibraryBookClient: Data4LibraryBookClient`로 교체, `naverItem.xxx` 참조를 새 필드명으로 교체.
- **설정**: `application.yml`에 `data4library.auth-key: ${DATA4LIBRARY_AUTH_KEY:}` 추가(`naver.*` 블록은 그대로 둠). `.env.example`에 `DATA4LIBRARY_AUTH_KEY=` 추가.
- **테스트**: `BookActionServiceTest.kt`, `BookServiceTest.kt`의 mock 대상을 `Data4LibraryBookClient`로 교체, `createNaverBookItem()` 헬퍼를 `createData4LibraryBookItem()`으로 대체. 기존 `NaverBookClient` 자체에 대한 별도 테스트는 없었으므로 신규 클라이언트 테스트만 추가하면 됨.

## 미해결/검증 필요

1. 발급받은 키가 아직 비활성 상태(`vitalizationErr`) — 활성화된 뒤 실제 `authors`/`bookImageURL` 응답 형식 재확인.
2. `srchDtlList`에서 `format=json`이 실제로 XML과 동일한 필드를 주는지(다른 정보나루 API에서는 확인했지만 이 API 자체 문서엔 명시 안 됨) — 활성화 후 확인.
3. `authors` 필드에 역할 접두어가 섞여 오면 파싱 규칙을 어떻게 할지(첫 번째 이름만 쓸지, 접두어를 정규식으로 제거할지) 결정 필요.

## 검증

1. `authKey` 활성화 확인 후 실제 ISBN으로 curl 호출 → 필드 매핑표와 실제 응답 대조
2. `./gradlew test --tests "*BookActionServiceTest" --tests "*BookServiceTest"`
3. 쿠러그 백엔드에 올라간 후 실제 도서 등록/미리보기 플로우 수동 확인
