# Schedule 테이블 마이그레이션

Schedule 도메인 변경(category enum 전환, location/expoint/endAt/checkCode 컬럼 추가)으로 인해 기존 `khlug_schedule` 테이블 대신 새 `schedule` 테이블을 생성했습니다.

기존 `khlug_schedule` 테이블은 데이터 마이그레이션을 위해 임시 보존 중입니다.

## 마이그레이션 절차

1. `khlug_schedule` 데이터를 `schedule` 테이블로 이전
   - `category` 컬럼: 숫자 코드 → enum 문자열로 변환
     - `7742` → `'CLUB'`
     - `7743` → `'ACADEMIC'`
     - `7744` → `'EXTERNAL'`
     - `32529`, `32530`, `32531` (ROOM_*) →  `GROUP_ACTIVITY`+`location='khlug_*'`
2. 마이그레이션 완료 및 검증 후 `khlug_schedule` 테이블 DROP
