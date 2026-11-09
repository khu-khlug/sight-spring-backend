# 신규 정수 식별자의 TSID 발급

## 결정과 범위

- Spring에서 새 정수 식별자를 발급하는 공통 방식으로 TSID를 사용한다. 다중 애플리케이션 인스턴스를 고려하며, 동일 테이블 안에서 유일해야 한다. 발급을 위해 레거시 전역 sequence 테이블을 도입하지 않는다.
- 기존 데이터의 식별자·참조·URL은 유지한다. SQL로 이전하는 행은 원래 식별자를 그대로 넣으며 TSID로 재채번하지 않는다.
- DB의 정수 식별자는 signed `BIGINT`, Kotlin 도메인은 `Long`을 사용한다. 현재 ULID 문자열을 사용하는 도메인과 스토리지 객체 키는 이번 결정으로 변경하지 않는다.
- TSID는 애플리케이션에서 생성해 도메인에 전달한다. 도메인이 시간·난수·발급 라이브러리를 직접 호출하지 않는다.
- TSID만으로 샤드 위치나 정확한 생성 순서를 결정하지 않는다. 소속 관계와 생성 일시는 기존 필드를 사용한다.
- 1차 적용은 그룹 시스템 구현에 한정한다. 기존 일정·매칭·내부 연동 등 API 전체의 문자열 전환과 기존 채번 교체는 후속 작업으로 분리하며, 대상과 교체 지점은 아래 도입 계획을 따른다. 기존 API가 새 그룹 시스템의 TSID를 읽는 공유 경로는 아래 호환성 조건을 먼저 충족해야 한다.

## 현재 구현

- ULID는 `com.github.f4b6a3:ulid-creator:5.2.3`의 `UlidCreator.getUlid()`를 각 서비스에서 호출한다.
- 정수 식별자는 `GenerationType.IDENTITY`와 그룹·그룹 로그·일정 서비스의 시간·랜덤 조합 방식이 혼재한다. 기능 브랜치에서는 그룹·로그가 `IDENTITY`로 바뀌었지만, 변경 전 코드로 발급한 값도 이전 대상으로 고려한다. 카드·리스트는 호출자가 식별자를 전달하는 도메인 기반만 있다.
- 기능 브랜치에 공통 TSID 발급 기능을 추가했다. `core/id/TsidGenerator.kt`는 `tsid-creator:5.2.6`을 사용하며 노드 10비트·epoch 2020 UTC를 고정한다. `service/IntegerIdService.nextId()`가 환경 변수로 지정한 노드 번호를 사용하고 기존 ID 경계를 검사한 뒤 Long을 반환한다.
- [그룹 공통 명세](../plans/group/global.md#아이디-타입)는 TSID 발급과 10진수 문자열 전송을 목표 계약으로 반영했다. 기존 업무 서비스의 채번 호출·숫자 응답 구현은 아직 전환하지 않았다. 공통 발급 기능은 기본 비활성화 상태이며 이번 추가만으로 운영 ID나 API 표현이 바뀌지 않는다.

### 공통 발급 기능의 적용 준비

- `INTEGER_ID_ENABLED=true`, `INTEGER_ID_NODE_ID=<JVM별 노드 번호>`, `INTEGER_ID_LEGACY_MAX_ID=<운영에서 확인한 기존 최댓값>`으로 활성화한다. 노드 번호는 0~1023이며 생략 시 무작위로 배정하지 않고 시작에 실패한다. 기존 데이터 조사 없이 예시값을 최댓값으로 사용하지 않는다.
- 활성화하면 시작 시 첫 생성 결과의 시간·양수 Long·기존 ID 경계를 확인한다. 이후 같은 JVM의 모든 호출이 생성기 하나를 공유한다. DB 노드 임대와 예약 상한 테이블·초기 SQL은 사용하지 않는다.
- 서비스가 생성자로 `IntegerIdService`를 주입받아 `nextId()`를 호출할 수 있는 기반까지 추가했다. 소비 서비스에 연결할 때는 아래 전환 순서에 따라 API 문자열 처리·사전 할당 저장 방식을 함께 적용한다. TSID Bean을 참조하는 서비스가 추가되면 해당 환경에서 활성화 설정도 필요하다.

### 배포 환경 변수 설정

`src/main/resources/application.yml`의 `integer-id` 설정은 각 Spring JVM이 아래 환경 변수를 읽는다.

| 환경 변수 | 설정값과 의미 |
| :-- | :-- |
| `INTEGER_ID_ENABLED` | TSID 발급 기능을 사용할 배포는 `true`. 미설정 기본값은 `false` |
| `INTEGER_ID_NODE_ID` | 해당 JVM에 배정한 정수 `0`~`1023`. 활성화 시 필수이며 동시에 실행되는 다른 JVM과 중복 불가 |
| `INTEGER_ID_LEGACY_MAX_ID` | 적용 대상의 원본·운영 데이터와 기존 발급 범위를 조사해 확정한 ID 상한의 10진수 값. 활성화 시 필수 |

운영은 [인프라 저장소의 ECS task definition](../../../sight-infra/spring-backend/task-definition.json)의 `containerDefinitions` 중 `sight-spring-backend` 컨테이너의 `environment` 배열에 세 항목을 추가한다. 아래 예시의 `<확인한 기존 ID 상한>`은 조사한 실제 숫자로 교체해야 한다. 이 값들은 발급 설정이며 노드 번호를 애플리케이션 이미지에 고정하지 않는다.

```json
[
  { "name": "INTEGER_ID_ENABLED", "value": "true" },
  { "name": "INTEGER_ID_NODE_ID", "value": "0" },
  { "name": "INTEGER_ID_LEGACY_MAX_ID", "value": "<확인한 기존 ID 상한>" }
]
```

- 노드 `0`은 JVM 하나에 배정하는 예시다. **같은 task definition으로 task 수만 늘리면 환경 변수도 복제되므로 노드 번호가 중복된다.** 여러 JVM을 운영할 때는 노드별로 다른 환경 값을 가진 task definition·서비스 등으로 배포 단위를 구분한다. 자동 확장으로 복제하려면 JVM별 번호를 공급하는 별도 배포 설계가 선행되어야 한다.
- 백엔드 CD의 `.github/workflows/cd.yml`은 이미지와 인프라 task definition의 이미지 참조를 갱신한다. workflow의 빌드 환경 변수에만 값을 추가하는 것으로 실행 중인 JVM에 전달되지는 않는다. task definition 변경은 새 리비전으로 등록하고 서비스에 적용한다. 이 문서 작업에서는 인프라 파일 변경이나 AWS 배포를 직접 수행하지 않는다.
- 백엔드는 일괄 배포한다. 동일 노드 번호를 다시 사용할 배포에서는 이전 task·JVM을 종료한 뒤 새 task를 시작하도록 운영한다. 같은 번호의 신·구 JVM이 겹치지 않게 하며 번호 재사용 시각은 아래 수명 규칙을 따른다.
- 로컬 `docker-compose.yaml`은 앱 컨테이너에서 `.env`를 `env_file`로 읽으므로 프로젝트 `.env`에 같은 세 항목을 설정할 수 있다. 공유 데이터에 쓰는 로컬 JVM이라면 운영 노드와도 번호를 구분한다. 여러 JVM을 한 컨테이너에서 실행한다면 실행 스크립트에서 `INTEGER_ID_NODE_ID`를 JVM별로 다르게 덮어쓴다.
- 활성화 시 노드 번호 누락·범위 오류, 기존 ID 상한 누락·범위 오류는 시작 단계에서 거부한다. TSID 발급 기능과 업무 API의 문자열 전환을 제공하는 배포에서 활성화한다.

## 도입 계획

### 1. 발급 대상과 호환성 확정

1차로 그룹 시스템의 신규 생성 경로에 공통 발급기를 적용하고 해당 API와 프런트에서 ID를 10진수 문자열로 처리한다. 기존 일정의 자체 채번과 기존 API 전체의 전환은 별도 TODO로 분리한다. 그룹 도메인의 기존 생성 경로를 공유하는 부분은 API 호환성을 확인한 범위에서 함께 대체하며, 기존 API의 채번만 먼저 TSID로 바꾸지 않는다.

각 대상의 기존 ID 범위와 레거시·Spring의 쓰기 경로를 확인한다. 백엔드는 한 번에 배포하며, 전환 대상의 기존 발급 요청·작업을 종료한 뒤 최종 ID 경계를 확인하고 새 백엔드에서 TSID 발급을 시작한다. 전환하지 않는 다른 대상의 기존 채번은 유지한다. 운영 데이터 조사는 담당자가 수행한다. 기존 ULID를 자르거나 해시해 정수로 만들지 않는다.

#### 기존 Spring 발급 범위 조사

레거시 ID는 약 십만까지 사용했다는 운영자 설명이 있으나, 이를 Spring 데이터까지 포함한 최댓값으로 간주하지 않는다. 문서 브랜치·기능 브랜치·로컬에 있는 `origin/main`과 관련 변경 이력에서 다음 발급식을 확인했다. 아래 날짜별 범위는 정상 시계가 가리키는 **2026-11-07 00:00:00 이상 2026-11-08 00:00:00 미만 KST**에 대한 계산이며, 실제 발급 건수·최댓값을 조회한 결과가 아니다.

| 대상 | 기존 발급식 | 해당 날짜에 가능한 범위 |
| :-- | :-- | :-- |
| 그룹 | `1,000,000 + 경과시간(시간) × 1,000 + random(0..999)` | `17,200,000`~`17,223,999` |
| 그룹 로그·일정 | `1,000,000 + 경과시간(분) × 1,000 + random(0..999)` | `973,000,000`~`974,439,999` |

- JavaScript의 안전한 정수 상한은 `9,007,199,254,740,991`이다. 위 기존 채번식의 현재 범위는 이보다 작으므로 정밀도 문제를 이유로 기존 채번까지 즉시 바꿀 필요는 없다. 반면 아래 TSID는 이미 이 상한을 넘으므로 최초 적용부터 문자열 계약이 필요하다. 이 판단은 코드의 정상 시각 계산 결과이며 운영 DB의 모든 저장값을 확인했다는 뜻은 아니다.
- 두 식의 기준 시각은 `2025-01-01T00:00:00+09:00`이다. 근거는 [그룹 생성](../../src/main/kotlin/com/sight/service/GroupMatchingService.kt)의 `createNewGroupId`, [로그 생성](../../src/main/kotlin/com/sight/service/GroupLogService.kt)의 `createNewGroupLogId`, [일정 생성](../../src/main/kotlin/com/sight/service/ScheduleService.kt)의 `createNewScheduleId`와 기존 구현 이력이다. 기능 브랜치의 `61d6c60`에서 그룹·로그 자체 채번은 제거되었으나 기존 데이터가 사라지는 것은 아니다.
- TSID Creator 기본 epoch `2020-01-01T00:00:00Z`의 구성식은 `(경과 밀리초 << 22) | 노드·카운터`다. 위 날짜의 시작 시각에 가능한 값은 `906,558,544,281,600,000`~`906,558,544,285,794,303`이다. 이 조건에서는 확인한 시간·랜덤 발급식의 기존 범위와 겹치지 않으며, signed Long의 양수 범위 안에 있다.
- 기존 발급식은 시간에 따라 계속 증가한다. 위 범위를 영구 상한으로 고정하지 않고 실제 전환 시각까지 다시 계산한다. 잘못된 서버 시각, 수동 입력, 다른 배포 코드, DB 자동 증가로 발급된 값은 이 계산만으로 상한을 보장할 수 없다.
- 전환 전 운영자는 적용 대상 테이블마다 `MIN(id)`·`MAX(id)`와 현재 `AUTO_INCREMENT` 값, 아직 가져오지 않은 원본 데이터의 최댓값을 확인한다. `IDENTITY`는 코드만으로 작은 발급 범위를 단정할 수 없다.
- 실제 기존 ID 상한과 전환 전까지의 이전 발급 범위를 포함한 경계 `B`를 정하고, TSID 활성화 시 허용하는 최초 시각에서의 최소 발급값이 `B`보다 큰지 확인한다. epoch를 최근으로 바꾸면 작은 값부터 시작할 수 있으므로 기본 epoch 계산 결과를 다른 설정에 그대로 적용하지 않는다. 발급기에도 `B` 이하 또는 양수 Long 범위 밖의 값을 거부하는 조건을 둔다.
- 전환 대상의 이전 발급 경로를 모두 중단한 뒤 신규 발급을 시작한다. 이 조건을 만족해야 기존 데이터와 TSID 범위가 겹치지 않는 것으로 판단한다.

### 2. 라이브러리와 발급기

JVM 라이브러리는 기존 ULID 라이브러리와 같은 제작자의 `com.github.f4b6a3:tsid-creator:5.2.6`으로 고정했다. MIT 라이선스와 공개 API·생성 소스를 확인했다. 재시작·시간 역행·카운터 소진 및 Java 17 환경의 실제 동작 확인은 담당자가 수행한다.

공통 정수 ID 발급기를 Spring 단일 인스턴스 Bean으로 구성한다. 서비스는 이 발급기에서 `Long`을 받아 도메인에 전달하고, 발급 라이브러리 직접 호출을 흩어 놓지 않는다. 동일 프로세스에서 같은 노드 번호의 독립 생성기를 여러 개 만들지 않는다.

### 3. 노드 번호와 수명

발급 프로세스마다 다른 노드 번호를 명시적으로 설정한다. 자동 무작위 선택과 IP 일부·해시로 할당하지 않는다. 모든 프로세스는 동일한 epoch·노드 비트 구성을 사용하고 signed Long의 양수 수명 범위 안에서 운영한다.

배포 설정에서 JVM마다 `INTEGER_ID_NODE_ID`를 직접 지정한다. 같은 컨테이너 안에서 JVM을 여러 개 실행하더라도 각각 다른 값을 전달한다. 기본값은 없으며 활성화 시 필수 값으로 검증한다. DB 노드 임대·하트비트·예약 상한 관리는 도입하지 않는다.

- 동시에 실행되는 JVM 사이의 번호 중복은 배포 담당자가 방지한다. 애플리케이션은 DB를 통한 중복 노드 탐지를 하지 않는다. 샤딩된 DB에 쓰는 프로세스라도 ID를 합칠 범위 안에서는 노드 번호를 구분한다.
- 같은 JVM의 모든 스레드·서비스는 Spring Bean의 TSID 생성기 하나를 공유한다. 노드 번호와 epoch·비트 구성은 프로세스 실행 중 변경하지 않는다.
- 번호를 재사용하기 전에 이전 JVM이 종료됐는지 확인한다. TSID 라이브러리의 마지막 논리 시각·카운터는 JVM 재시작 후 유지되지 않으므로 시간 역행이나 이전 논리 시각을 따라잡지 못한 재시작까지 자동 보호한다고 가정하지 않는다. 그 상황에서는 다른 미사용 노드 번호를 쓰거나 이전 발급 시각을 지난 뒤 재개한다.
- 기존 ID는 유지하며 epoch·비트 구성 변경은 기존 데이터와의 호환성 변경으로 다룬다. 시스템 시각과 생성 ID의 양수 Long 수명 검사는 유지한다.

### 4. API와 프런트 전환

TSID를 담는 정수 식별자는 JSON에서 10진수 문자열로 전송하고 프런트에서도 문자열로 유지하는 방향으로 계약을 변경한다. DB와 도메인의 정수 타입은 유지한다. 기존 ID `123`도 같은 대상의 문자열 `"123"`으로 전달하며 경로와 링크의 식별자 값은 바꾸지 않는다.

Base32 대신 10진수 표기를 사용하는 이유는 레거시 식별자와 URL의 호환성이다. 기존 숫자 ID의 표기를 유지하고, 새 TSID도 같은 10진수 식별자로 취급한다. Base32로 변환해 기존 링크나 참조 값을 바꾸지 않는다. JSON 문자열로 전송하는 이유는 이 표기 선택과 별개로 JavaScript의 정수 정밀도를 보존하기 위해서다.

숫자 JSON을 받은 뒤 `String()`으로 바꾸는 것은 이미 발생한 정밀도 손실을 복구하지 못한다. 응답·요청·중첩 참조·로그 데이터·목업·로컬 저장에서 TSID가 JavaScript `Number`를 통과하지 않도록 함께 변경한다. 모든 Long 값이나 수량을 일괄 문자열화하지 않는다.

그룹 플랜에는 10진수 문자열 표현을 반영했으며 실제 응답 구현·프런트는 아직 전환하지 않았다. 1차 배포에서는 그룹 시스템의 해당 API와 소비 프런트에 문자열 표현과 TSID 발급을 함께 반영한다. 그 밖의 기존 API·소비자·채번 교체는 별도 TODO에서 함께 수행한다.

#### 기존 API와 데이터를 공유할 때의 조건

- API 이름이나 구현 시점만으로 전환 범위를 나누지 않는다. 기존 `GET /groups`도 같은 그룹 테이블을 읽고, `GET /groups/{groupId}/logs`도 같은 로그 테이블을 읽는다. 새 TSID 행이 기존 숫자 응답에 포함되면 기존 채번식의 값이 작아도 정밀도가 손실된다.
- TSID를 읽을 수 있는 기존 API·중첩 참조와 그 소비자는 해당 TSID 쓰기를 활성화하기 전에 함께 전환해야 한다. 해당 전환을 TODO로 미루는 동안에는 그 경로가 노출하는 대상의 TSID 신규 발급도 활성화하지 않는다. 카드·리스트·기록부터 적용하더라도 함께 생성하는 활동 로그와 그 조회 경로까지 확인한다.
- 기능 브랜치의 그룹·기록·로그는 현재 `IDENTITY`를 사용한다. 같은 자동 증가 컬럼에 큰 TSID를 직접 넣으면 이후 자동 증가 값도 커질 수 있으므로, 같은 테이블의 다른 생성 경로를 작은 ID로 유지할 수 있다고 가정하지 않는다. 테이블 단위로 모든 생성 경로의 사전 할당 방식과 신규 저장 판정을 함께 정리한다.
- 따라서 일정 등 별도 대상의 기존 채번은 이번에 바꾸지 않는다. 그룹 도메인의 기존 생성·조회 경로는 새 시스템과 공유되는 범위만 함께 처리하고, 미전환 경로가 남는 대상은 TSID 쓰기를 보류한다.

### 5. 전환과 검증

#### 기존 발급 제거와 대체 순서

1. 한 번 배포할 새 백엔드에 TSID 라이브러리·공통 발급기·JVM별 노드 번호 설정·기존 ID 경계 검사와 문자열 API 표현을 함께 구현한다. 기존 값은 갱신하지 않고 신규 생성 경로에만 발급기를 주입한다.
2. 1차 전환 대상의 기존 시간·랜덤 생성 함수와 채번 전용 `existsById` 재시도를 제거한다. 기존 일정 등 미전환 대상의 채번은 별도 TODO에서 교체한다. 조회 후 삽입은 원자적인 중복 방지가 아니므로 TSID의 대체 장치로 남기지 않는다. DB 유일 제약은 유지하며 TSID 중복은 정상 충돌로 숨기지 않고 운영 오류로 처리한다.
3. 기능 브랜치의 `Group`, `GroupRecord`, `GroupLog`는 생성 시 ID를 받아 저장하도록 바꾸고 `@GeneratedValue(IDENTITY)`를 제거한다. 이미 생성자로 ID를 받는 `GroupList`, `GroupCard`, `Schedule`은 생성 서비스에서 공통 발급기를 호출한다. SQL로 이전하는 기존 ID는 그대로 유지한다.
4. Spring Data JPA가 미리 할당된 ID를 기존 엔티티로 오인해 merge하지 않도록 신규 판정을 명시한다. `Persistable` 또는 명시적인 신규 저장 경로 중 저장소 규칙에 맞는 방식을 선택하고, 기존 행을 실수로 갱신하지 않도록 한다.
5. 전환 시 이전 백엔드와 전환 대상의 기존 발급 작업을 종료하고, 최종 ID 상한·DB 변경·노드 할당 상태를 확인한 뒤 새 백엔드를 시작한다. 대상 API 소비자의 10진수 문자열 계약도 이에 맞춰 전환한다. 전환 대상의 구 채번 코드·재시도·관련 설정은 제거한다. 레거시 sequence 테이블은 다른 사용처가 남아 있는 동안 삭제하지 않는다.
6. 모든 자동 증가 컬럼을 일괄 변경하지 않는다. 1차 대상은 그룹 시스템의 그룹·리스트·카드·기록·그룹 로그이며 위 공유 경로 조건을 적용한다. 일정과 기존 API 전반은 별도 TODO이며, 회원·기타 업무와 내부 연결 행은 일괄 전환하지 않는다. 연관관계의 Long 타입과 기존 PK·유일 제약은 유지한다.

#### 기존 동작에서 교체할 지점

아래는 최종 전환 시 바뀌어야 하는 기존 채번·숫자 전송·숫자 해석 동작의 조사 목록이다. 전부 이번 구현 범위라는 뜻은 아니며, 그룹 시스템의 공유 경로를 제외한 기존 API 전환은 별도 TODO에서 진행한다. 백엔드 경로는 `src/main/kotlin/com/sight/` 기준이다. 문서 브랜치·기존 main의 구현과 기능 브랜치의 도메인 변경을 구분한다.

**기존 정수 채번**

| 기존 위치·동작 | 교체할 동작 |
| :-- | :-- |
| `service/GroupMatchingService.kt`의 `createNewGroupId()` | 2025년 기준 경과시간·랜덤 조합을 제거하고 `createGroupFromGroupMatching()`에서 `IntegerIdService.nextId()` 사용 |
| `service/GroupLogService.kt`의 `createNewGroupLogId()`와 채번용 `existsById` 재시도 | 분 단위 시간·랜덤 조합과 조회 후 후보 재발급을 제거. 로그 저장 전에 공통 발급기 호출 |
| `service/ScheduleService.kt`의 `createNewScheduleId()`·`pickAvailableScheduleId()` | 분 단위 시간·랜덤 조합과 채번 전용 재시도 제거. 일반 일정·그룹 활동·빅세미나 일정이 공유하는 생성 경로에서 공통 발급기 호출 |
| 기능 브랜치의 `domain/group/Group.kt`, `GroupRecord.kt`, `GroupLog.kt`의 `@GeneratedValue(IDENTITY)`와 `id = 0` | TSID를 생성자에서 받아 저장하도록 변경. 그룹·로그 자체 채번이 기능 브랜치에서 자동 증가로 바뀌었어도 최종 목표는 TSID 사전 할당 |

카드·리스트는 기능 브랜치에서 이미 Long ID를 생성자로 받으며 기존 Spring 생성 서비스는 아직 없다. 따라서 제거할 기존 채번 함수가 있는 대상으로 적지 않고, 신규 생성 서비스를 구현할 때 공통 발급기를 사용한다. 일반적인 존재 확인 메서드나 회원·내부 연결 행의 자동 증가를 이 목록 때문에 일괄 제거하지 않는다.

**Long ID를 JSON 숫자로 보내는 기존 응답**

아래 필드는 현재 Long을 응답에 그대로 넣어 JSON 숫자가 된다. DB·도메인·서비스 내부는 Long으로 유지하고 해당 API 필드와 컨트롤러의 응답 변환을 10진수 문자열로 바꾼다. 최상위 `id`뿐 아니라 다른 리소스 안의 참조도 포함한다.

| 기존 DTO·필드 | 연결된 기존 응답 경로 |
| :-- | :-- |
| `ListGroupsResponse.kt`의 `GroupResponse.id` | `GroupController`의 그룹 목록 |
| `CreateGroupResponse.kt`의 `id` | `GroupController.createGroup()`의 그룹 생성 결과 |
| `GetGroupMatchingGroupsResponse.kt`의 최상위 `id` | `GroupMatchingController`의 매칭 그룹 조회. 같은 파일의 회원 `id`·`userId`는 대상 아님 |
| `GetGroupMatchingAnswerResponse.kt`, `CreateGroupMatchingAnswerResponse.kt`의 `matchedGroups[].groupId` | 매칭 응답 조회·생성·수정에 중첩된 그룹 참조. 매칭·응답·연결 행의 ULID `id`는 유지 |
| `ListGroupLogResponse.kt`의 `id` | `GroupLogController`의 활동 로그. 기능 브랜치에서 로그 응답 형태가 달라져도 로그 ID 변환은 동일하게 적용 |
| `ActivityReportResponse.kt`, `ListActivityReportsResponse.kt`의 `groupId` | `GroupActivityReportController`의 활동보고 생성·수정·목록. 보고서·세미나 ID는 기존 문자열 유지 |
| `CreateGroupDiscordChannelResponse.kt`, `GetGroupDiscordChannelResponse.kt`의 `groupId` | 공개 채널 생성 응답과 `InternalGroupController`의 내부 채널 조회. 내부 API 소비자도 변경된 그룹 참조 표현에 대응 필요 |
| `CreateScheduleResponse.kt`의 `id` | `ScheduleController`, `GroupActivityScheduleController`, `BigSeminarScheduleController`의 생성·수정 응답 |
| `GetScheduleResponse.kt`의 `id`·`groupId`, `ListSchedulesResponse.kt`의 `ScheduleDto.id`·`groupId` | 일정 단건·목록. 같은 `ScheduleDto`를 쓰는 `ListActiveSchedulesResponse.kt`의 `schedules`·`schedule`에도 적용 |
| `CreateScheduleAttendanceResponse.kt`의 `scheduleId` | 출석 등록 응답이 참조하는 일정 ID |

회원의 `userId`·`memberId`·`author`·담당자 ID, 학번, `count`·인원 수·경험치·페이지 번호는 이번 전환 대상이 아니다. Long 전체에 전역 문자열 직렬화를 적용하지 않는다.

**JSON 숫자로 받는 기존 참조**

`CreateGroupActivityScheduleRequest.kt`의 `groupId: Long`은 현재 JSON 숫자 요청을 받는다. 그룹 ID를 10진수 문자열로 받아 양의 Long 범위를 검사한 뒤 서비스에 전달하도록 바꾼다. URL 경로의 `@PathVariable groupId: Long`·`scheduleId: Long`은 원래 문자열 경로를 해석하므로, 도메인 타입을 String으로 바꿀 이유가 없다. 신규 카드·기록 API의 본문 참조는 그룹 플랜의 문자열 계약을 따른다.

**프런트의 기존 숫자 해석**

`sight-frontend/src/` 기준으로 확인한 변경 지점은 다음과 같다.

| 기존 위치·동작 | 교체할 동작 |
| :-- | :-- |
| `pages/member/group/detail/index.tsx`의 `Number(groupId)` | URL의 그룹 ID를 문자열로 유지하고 10진수 형식 검증 |
| `features/member/GroupDetailContainer/index.tsx`의 기록 삭제·복구·수정 호출에서 `Number(recordId)` | 기록 ID 문자열을 API에 그대로 전달 |
| `features/member/GroupDetailContainer/EditWindow/index.tsx`의 기록 조회·임시저장 대상·생성 후 재시도에서 `Number(recordId)` | 기록 조회·초안·재시도 대상 모두 문자열로 유지 |
| `api/public/group/`의 `groupId: number`·`recordId: number`, `KanbanApi.ts`의 기록 `id: number` | 대상 ID와 생성 응답·API 인자를 문자열로 변경. 기존 문자열 카드·리스트 ID와 회원 숫자 ID는 유지 |
| `features/member/GroupDetailContainer/EditWindow/useRecordDraft.ts`와 `api/public/group/AutosaveApi.ts`의 숫자 기록·그룹 ID 및 숫자 타입 검사 | 초안의 대상 ID도 문자열로 저장·복구. 그룹 상세는 미배포 상태이므로 이전 브라우저 저장 데이터의 호환 처리는 제공하지 않음 |

역할 번호·창 정렬·파일명 파싱 등에 쓰는 무관한 `Number()`·`parseInt()`까지 제거하지 않는다. 일정·그룹 목록 소비자와 개발 목업에도 위 응답 변경을 적용하되 실제 ID 처리 지점을 기준으로 변경한다. 이 절은 확인한 기존 동작의 교체 목록이며 새 API 전체의 구현 파일 목록은 아니다.


기존 식별자와 URL을 보존한 상태에서 API 표현 변경을 제공하고, 대상별 발급기를 전환한다. 애플리케이션 할당 대상의 `IDENTITY` 선언·DB 자동 증가·저장 시 신규 엔티티 판정도 함께 조정한다. 롤백 시에도 이미 발급된 TSID를 유지하고 이전 채번기가 그 값과 충돌하지 않는지 확인한다.

담당자는 동시 발급·같은 버전의 서버 간 발급·재시작·시간 역행·카운터 소진·기존 ID와의 공존·브라우저 왕복 정밀도를 확인한다. 전환 시 이전 발급 작업이 종료됐는지도 확인한다. DB PK·유일 제약은 최종 방어로 유지한다. 에이전트의 테스트·타입 검사·브라우저 조작은 사용자 요청이 있을 때만 실행한다.

## 근거

- [TSID Creator 구조·노드 설정](https://github.com/f4b6a3/tsid-creator): 64비트 표현, 노드 번호와 카운터 구성, 명시적 노드 할당 및 signed 정수 수명.
- [ECMAScript의 안전한 정수 범위](https://tc39.es/ecma262/multipage/numbers-and-dates.html#sec-number.max_safe_integer): JavaScript Number의 안전한 정수 상한은 `2^53 - 1`이다.
