# Backend Task 문서 작성 Standard

## 목적

Backend Task 문서는 작업으로 인해 달라지는 계약, 비즈니스 동작, 비가역적 결정을 사람이 구현 전에 검토할 수 있도록 설명한다.

사람은 다음 내용을 검토한다.

- HTTP API 계약
- database 계약
- 외부 시스템 계약
- 사용자에게 관찰되는 비즈니스 동작
- 보안, 개인정보 및 비가역적 부작용
- 호환성 및 배포 전략

내부 구현은 Task 문서와 `rules/`가 허용하는 범위에서 구현자가 결정한다. 반복되는 구현 문제는 `rules/`로, 기계적으로 판별할 수 있는 규칙은 lint로 관리한다.

## 저장 위치와 상태

- 완료되지 않은 Task 문서는 `tasks/open/`에 둔다.
- 완료된 Task 문서는 `tasks/completed/`로 옮긴다.
- 이 문서의 `필수 섹션` 절은 Task 문서의 필수 섹션과 순서를 정의한다.

## 작성 원칙

### 도메인 용어로 동작을 설명한다

비즈니스 동작은 저장소와 이해관계자가 공유하는 유비쿼터스 언어로 작성한다.

- 동작의 주체와 대상
- 허용 및 거부 조건
- 상태 전이
- 저장하거나 전달하는 데이터
- 외부에서 관찰되는 성공과 실패

일반적인 기술 용어, 비즈니스 용어, 저장소에서 영어로 사용하는 용어는 그대로 사용한다. 각 문장만으로 의미와 다음 동작을 예측할 수 있게 작성한다.

HTTP field, database schema, configuration key, event field 등 계약에 포함되는 identifier는 정확한 이름을 작성한다.

### 변경과 유지를 명시한다

모든 필수 영역에 변경 내용을 작성하거나 `변경 없음`을 명시한다.

작업 중 달라질 가능성이 있는 기존 계약은 유지 조건을 작성한다.

```markdown
## HTTP API 계약

변경 없음

### 유지 조건

- 가입 신청이 존재하지 않을 때의 `404 Not Found` 응답을 유지한다.
```

### 미결정 사항을 명시한다

사람의 결정이 필요한 사항은 `사람의 결정 필요` 섹션에 질문, 선택지, 영향, 권장안을 작성한다.

## 필수 섹션

모든 Task 문서는 아래 섹션을 같은 이름과 순서로 작성한다.

### 1. 작업 개요

- 해결하려는 문제 또는 제공하려는 기능
- 작업의 대상 사용자 또는 시스템
- 완료 여부를 판단할 수 있는 결과

### 2. 비즈니스 동작

도메인 용어로 다음 내용을 작성한다.

- 동작의 시작 조건
- 주요 상태와 상태 전이
- 허용 및 거부 조건
- 성공 결과
- 실패 결과
- 중복 요청 또는 재시도 동작

변경이 없다면 `변경 없음`을 작성한다.

### 3. HTTP API 계약

API마다 다음 내용을 작성한다.

- HTTP method와 path
- 인증 및 인가 조건
- path, query, header, cookie parameter
- request body
- response status와 body
- error status와 외부에서 구분 가능한 error
- field 이름, type, 필수 여부, nullable 여부 및 의미
- pagination, filtering, sorting
- 멱등성 및 중복 요청 처리
- 기존 API와의 호환성

해당하지 않거나 변경이 없다면 `변경 없음`을 작성한다.

### 4. 데이터베이스 계약

- 추가, 변경, 삭제하는 table과 column
- type, 길이 또는 precision
- nullable 여부와 default
- primary key, foreign key, unique 및 check constraint
- index
- 제한된 저장값과 각 값의 의미
- 연관관계와 삭제 정책
- 기존 데이터에 적용할 migration 또는 backfill
- 대상 데이터 규모와 lock 또는 성능 위험
- 구버전과 신버전 application의 동시 실행 호환성
- rollback 가능 여부와 방법

해당하지 않거나 변경이 없다면 `변경 없음`을 작성한다.

### 5. 외부 시스템 계약

외부 연동마다 다음 내용을 작성한다.

- 대상 시스템과 연동 목적
- request, response, event 또는 message payload
- 인증 방식과 필요한 권한
- timeout, retry 및 rate limit 처리
- 중복 전달과 재처리 동작
- 부분 실패 동작
- 기존 consumer 또는 provider와의 호환성

해당하지 않거나 변경이 없다면 `변경 없음`을 작성한다.

### 6. 보안 및 개인정보

- 인증 및 인가 범위
- 수집, 저장, 반환 또는 전달하는 개인정보와 민감정보
- log, metric, tracing에 포함되는 민감정보
- 데이터 보존 및 삭제 조건
- 권한 상승 또는 정보 노출 가능성

해당하지 않거나 변경이 없다면 `변경 없음`을 작성한다.

### 7. 비가역적 부작용 및 운영 영향

- 데이터 삭제 또는 대량 변경
- 외부 알림과 message 발송
- 비용을 발생시키는 외부 호출
- batch와 scheduler의 실행 조건
- 예상 호출량 또는 처리량 변화
- 장애 시 중단, 재개 및 복구 방법
- 필요한 log, metric 또는 alert

해당하지 않거나 변경이 없다면 `변경 없음`을 작성한다.

### 8. 호환성 및 배포

- 하위 호환 여부
- 함께 변경할 consumer
- 단계적 배포 또는 feature flag
- migration, backfill, application 배포 순서
- 구버전과 신버전이 함께 실행되는 동안의 동작
- rollback 조건과 제한

고려할 내용이 없다면 `변경 없음`을 작성한다.

### 9. 검증

- 주요 성공 흐름
- 주요 실패 흐름
- 권한
- 경계값 및 상태 전이
- 중복 요청과 재시도
- migration 및 기존 데이터
- 외부 시스템 실패

각 항목의 기대 결과와 검증 방법을 작성한다.

### 10. 사람의 결정 필요

결정마다 다음 내용을 작성한다.

- 질문
- 선택지
- 각 선택지의 계약, 운영 및 호환성 영향
- 권장안과 이유

결정할 내용이 없다면 `없음`을 작성한다.

### 11. 구현자 재량

구현자가 자유롭게 정할 수 있는 범위와 추가 제한을 작성한다.

일반적인 구현자 재량:

- 내부 class, method 및 variable 이름
- private 함수 분리
- 내부 제어 흐름
- 동일한 결과를 만드는 query 또는 mapping 구현
- 보조 type과 파일 구성
- `rules/`가 허용하는 library

계약에 포함되는 identifier:

- HTTP 및 serialization field 이름
- table, column, index 및 constraint 이름
- event와 message field 이름
- configuration key와 environment variable 이름
- 명시적인 이름으로 조회되는 Spring bean
- metric, log field 및 tracing attribute 이름
- 저장소 밖에서 참조하는 공개 identifier

추가 제한이 없다면 `추가 제한 없음`을 작성한다.

### 12. 비목표

이번 작업에서 다루지 않는 인접 영역을 작성한다.

비목표가 없다면 `없음`을 작성한다.

## 사람의 검토가 필요한 변경

- HTTP API 계약의 추가, 변경 또는 삭제
- 인증 또는 인가 조건 변경
- database schema, constraint, index 또는 저장값 의미 변경
- migration 또는 backfill
- 외부 시스템 계약 변경
- 사용자에게 관찰되는 비즈니스 흐름 또는 규칙 변경
- 개인정보 또는 민감정보 처리 변경
- 데이터 삭제, 대량 변경, 외부 발송 등 비가역적 부작용
- 호환되지 않는 변경
- 요구사항의 모호함

## Task 문서, `rules/` 및 lint의 역할

- Task 문서는 이번 작업의 계약, 비즈니스 의미와 승인 경계를 기록한다.
- `rules/`는 여러 작업에 반복 적용할 구현 가드레일을 정의한다.
- lint는 필수 내용의 존재와 문서 형식을 deterministic하게 검사한다.
- 사람은 계약, 비즈니스 의미와 결정을 검토한다.

현재 lint는 `tasks/open/`과 `tasks/completed/`의 Markdown 문서에 대해 다음 내용을 검사한다.

- 이 문서의 `필수 섹션` 절에 정의된 모든 섹션이 정확히 한 번 존재한다.
- 필수 섹션끼리의 상대적 순서가 같다.
- 각 필수 섹션에 공백을 제외한 내용이 한 글자 이상 존재한다.

추가 `##` 섹션은 허용한다.
