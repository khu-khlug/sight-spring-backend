# 저장소 기능에서 사용하는 GitHub API

기본 주소: `https://api.github.com`. 아래 출력은 GitHub 응답 중 저장소 기능에 필요한 필드만 발췌한 것이다. 모든 요청은 GET이며 요청 바디는 없다.

## 공통 입력·출력 헤더

| 방향 | 헤더 | 값·의미 |
| :-- | :-- | :-- |
| 입력 | `Authorization` | `Bearer {공용 토큰}` |
| 입력 | `Accept` | `application/vnd.github+json` |
| 입력 | `X-GitHub-Api-Version` | 백엔드가 사용하는 GitHub API 버전 |
| 입력 | `If-None-Match` | 동일 요청 조건으로 저장한 ETag가 있을 때 원문 값 |
| 입력 | `If-Modified-Since` | ETag가 없고 Last-Modified가 있을 때 저장된 값 |
| 출력 | `ETag` | 해당 응답의 검증자. 제공되는 경우 보관 |
| 출력 | `Last-Modified` | 최종 변경 시각. 제공되는 경우 보관 |
| 출력 | `Link` | 페이지가 있는 API의 다음 페이지 등 링크 |
| 출력 | `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `X-RateLimit-Used`, `X-RateLimit-Resource` | GitHub 호출 한도 정보 |
| 출력 | `Retry-After` | 재시도 대기 시간. 제공되는 경우 사용 |

조건부 요청의 `304 Not Modified`에는 본문이 없다. 아래 본문 스키마는 `200 OK` 기준이다.

## 저장소 정보

`GET /repos/{owner}/{repo}`

입력:

| 위치 | 이름 | 타입 | 의미 |
| :-- | :-- | :-- | :-- |
| 경로 | `owner` | 문자열 | 저장소 소유자 |
| 경로 | `repo` | 문자열 | `.git`을 제외한 저장소 이름 |

출력:

```typescript
{
  default_branch: string; // 기본 브랜치 이름
}
```

브랜치 목록 응답에는 기본 브랜치 표시가 없으므로 이 값과 브랜치 이름을 비교한다.

[GitHub 문서](https://docs.github.com/en/rest/repos/repos#get-a-repository)

## 브랜치 목록

`GET /repos/{owner}/{repo}/branches`

입력:

| 위치 | 이름 | 타입 | 의미 |
| :-- | :-- | :-- | :-- |
| 경로 | `owner`, `repo` | 문자열 | 저장소 소유자와 이름 |
| 쿼리 | `page` | 정수 | 1부터 시작. 기본값 1 |
| 쿼리 | `per_page` | 정수 | 기본값 30, 최대 100 |

출력 본문은 배열이다. 다음 페이지는 `Link` 헤더로 확인한다.

```typescript
{
  name: string;
  commit: {
    sha: string; // 브랜치 HEAD 커밋 SHA
  };
}[]
```

[GitHub 문서](https://docs.github.com/en/rest/branches/branches#list-branches)

## 브랜치 커밋 목록

`GET /repos/{owner}/{repo}/commits`

입력:

| 위치 | 이름 | 타입 | 의미 |
| :-- | :-- | :-- | :-- |
| 경로 | `owner`, `repo` | 문자열 | 저장소 소유자와 이름 |
| 쿼리 | `sha` | 문자열 | 조회할 브랜치 이름 또는 시작 커밋 SHA. 생략하면 기본 브랜치 |
| 쿼리 | `page` | 정수 | 1부터 시작. SIGHT 요청의 `page + 1` |
| 쿼리 | `per_page` | 정수 | SIGHT는 30으로 고정 |

출력 본문은 배열이며 전체 커밋 수는 제공하지 않는다. 다음 페이지는 `Link` 헤더의 `rel="next"`로 확인한다.

```typescript
{
  sha: string;
  commit: {
    message: string;
    tree: {
      sha: string; // 해당 커밋의 루트 트리 SHA
    };
  };
}[]
```

[GitHub 문서](https://docs.github.com/en/rest/commits/commits#list-commits)

## 단일 커밋의 루트 트리

`GET /repos/{owner}/{repo}/git/commits/{commit_sha}`

입력:

| 위치 | 이름 | 타입 | 의미 |
| :-- | :-- | :-- | :-- |
| 경로 | `owner`, `repo` | 문자열 | 저장소 소유자와 이름 |
| 경로 | `commit_sha` | 문자열 | 대상 커밋 SHA |

출력:

```typescript
{
  sha: string;
  tree: {
    sha: string; // 파일 트리 조회에 사용할 루트 트리 SHA
  };
}
```

파일 트리 요청의 커밋 SHA에 대응하는 루트 트리 SHA를 아직 확보하지 못한 경우 사용한다. 커밋 SHA와 트리 SHA는 서로 다른 식별자다.

[GitHub 문서](https://docs.github.com/en/rest/git/commits#get-a-commit)

## 파일·디렉터리 트리

`GET /repos/{owner}/{repo}/git/trees/{tree_sha}`

입력:

| 위치 | 이름 | 타입 | 의미 |
| :-- | :-- | :-- | :-- |
| 경로 | `owner`, `repo` | 문자열 | 저장소 소유자와 이름 |
| 경로 | `tree_sha` | 문자열 | 루트 또는 하위 디렉터리의 트리 SHA |
| 쿼리 | `recursive` | 문자열 | `1` 등 값이 있으면 하위 전체 재귀 조회. 생략하면 해당 디렉터리의 바로 아래 항목만 조회 |

`recursive=0`이나 `recursive=false`도 재귀 조회를 활성화한다. 비재귀 조회에서는 파라미터 자체를 생략한다. `page`·`per_page`·깊이 지정 옵션은 없다.

출력:

```typescript
{
  sha: string;
  truncated: boolean;
  tree: {
    path: string; // 요청한 트리 기준 상대 경로
    mode: string;
    type: "blob" | "tree" | "commit";
    sha: string;
    size?: number; // 파일 크기(바이트)
    url?: string; // Git 객체 API 주소. 파일 원문 주소가 아님
  }[];
}
```

- 재귀 응답은 최대 100,000개 항목 또는 7MB 제한에 걸리면 `truncated=true`다. 깊이 제한이 아니며 얕은 경로부터 반환된다는 보장은 없다.
- 잘린 응답의 후속 페이지·커서는 없다. 더 필요하면 루트 트리를 비재귀 조회하고, `type="tree"` 항목의 `sha`로 하위 트리를 각각 비재귀 조회한다. 하위 조회 결과의 경로는 상위 디렉터리 경로와 합쳐야 한다.
- GitHub의 `blob`·`tree`는 SIGHT 응답의 `file`·`dir`에 대응한다. `commit`은 서브모듈이므로 일반 파일·디렉터리로 단순 치환하지 않는다.
- `rawUrl`은 이 API의 출력 필드가 아니다. 공개 저장소의 파일 원문 주소는 저장소 정보·대상 커밋 SHA·파일 경로로 구성한다.

[GitHub 문서](https://docs.github.com/en/rest/git/trees#get-a-tree)
