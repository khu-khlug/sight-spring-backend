# obsoletedAt, obsoleteReason 필드 추가 영향 분석

## 📋 수정이 필요한 파일 목록

### 1. ✅ 엔티티 (주요 변경)
**파일**: `src/main/kotlin/com/sight/domain/groupmatching/GroupMatchingField.kt`

**변경사항**:
```kotlin
@Entity
@Table(name = "group_matching_field")
data class GroupMatchingField(
    @Id val id: String,
    @Column(name = "name") val name: String,
    @Column(name = "created_at") var createdAt: LocalDateTime = LocalDateTime.now(),
    
    // 신규 추가
    @Column(name = "obsoleted_at") var obsoletedAt: LocalDateTime? = null,
    @Column(name = "obsolete_reason", length = 1000) var obsoleteReason: String? = null,
)
```

**주의**: `var`로 변경 필요 (재활성화 로직에서 수정)

---

### 2. ⚠️ Repository (쿼리 메서드 추가)
**파일**: `src/main/kotlin/com/sight/repository/GroupMatchingFieldRepository.kt`

**현재**:
```kotlin
interface GroupMatchingFieldRepository : JpaRepository<GroupMatchingField, String> {
    fun existsByName(name: String): Boolean
}
```

**수정 후**:
```kotlin
interface GroupMatchingFieldRepository : JpaRepository<GroupMatchingField, String> {
    fun existsByName(name: String): Boolean
    
    // 신규 추가
    fun findByName(name: String): GroupMatchingField?
    fun existsByNameAndObsoletedAtIsNull(name: String): Boolean
    
    // 활성 상태만 조회 (목록 API용)
    fun findAllByObsoletedAtIsNull(): List<GroupMatchingField>
}
```

**영향**:
- 기존 `existsByName`은 obsoleted된 것도 포함
- 활성 상태만 체크하려면 `existsByNameAndObsoletedAtIsNull` 사용

---

### 3. 🔴 Service - addGroupMatchingField (필수 수정)
**파일**: `src/main/kotlin/com/sight/service/GroupMatchingFieldService.kt`

**현재 로직**:
```kotlin
fun addGroupMatchingField(request: AddGroupMatchingFieldRequest): GroupMatchingField {
    if (groupMatchingFieldRepository.existsByName(request.fieldName)) {
        throw UnprocessableEntityException("이미 존재하는 관심분야 이름입니다")
    }
    
    val field = GroupMatchingField(
        id = UlidCreator.getUlid().toString(),
        name = request.fieldName,
    )
    
    return groupMatchingFieldRepository.save(field)
}
```

**문제점**:
- `existsByName`은 obsoleted된 필드도 체크함
- 재활성화 로직 없음

**수정 필요**:
```kotlin
fun addGroupMatchingField(request: AddGroupMatchingFieldRequest): GroupMatchingField {
    val existingField = groupMatchingFieldRepository.findByName(request.fieldName)
    
    // 활성 상태 중복
    if (existingField != null && existingField.obsoletedAt == null) {
        throw UnprocessableEntityException("이미 존재하는 관심분야 이름입니다")
    }
    
    // 폐기 상태 → 재활성화
    if (existingField != null && existingField.obsoletedAt != null) {
        existingField.obsoletedAt = null
        existingField.obsoleteReason = null
        existingField.createdAt = LocalDateTime.now()
        return groupMatchingFieldRepository.save(existingField)
    }
    
    // 새로 생성
    val field = GroupMatchingField(
        id = UlidCreator.getUlid().toString(),
        name = request.fieldName,
    )
    
    return groupMatchingFieldRepository.save(field)
}
```

---

### 4. 🔴 Service - deleteGroupMatchingField (필수 수정)
**파일**: `src/main/kotlin/com/sight/service/GroupMatchingFieldService.kt`

**현재 로직** (Hard Delete):
```kotlin
fun deleteGroupMatchingField(fieldId: String) {
    if (!groupMatchingFieldRepository.existsById(fieldId)) {
        throw NotFoundException("존재하지 않는 관심분야입니다")
    }
    
    groupMatchingFieldRepository.deleteById(fieldId)
}
```

**수정 필요** (Soft Delete):
```kotlin
fun deleteGroupMatchingField(fieldId: String) {
    val field = groupMatchingFieldRepository.findById(fieldId)
        .orElseThrow { NotFoundException("존재하지 않는 관심분야입니다") }
    
    field.obsoletedAt = LocalDateTime.now()
    field.obsoleteReason = "운영진 삭제"
    
    groupMatchingFieldRepository.save(field)
}
```

---

### 5. 📝 테스트 코드 수정
**파일**: `src/test/kotlin/com/sight/service/GroupMatchingFieldServiceTest.kt`

**추가할 테스트**:
1. 폐기된 필드 재활성화 테스트
2. 활성 상태 중복 체크 테스트
3. Soft delete 테스트

---

### 6. ❓ 향후 추가될 API (고려 필요)

#### 6-1. 관심분야 목록 조회 API
**URL**: `GET /fields`

**현재는 없지만**, 추가될 경우:
```kotlin
fun getAllFields(): List<GetFieldResponse> {
    // ❌ 잘못된 예: obsoleted된 것도 포함
    // return repository.findAll().map { it.toResponse() }
    
    // ✅ 올바른 예: 활성 상태만 조회
    return repository.findAllByObsoletedAtIsNull().map { it.toResponse() }
}
```

#### 6-2. 관심분야 추가 요청 승인 API
**가능성**: 향후 `GroupMatchingFieldRequest` 승인 시 `GroupMatchingField` 생성

**고려사항**:
- 승인 시 기존 필드가 폐기 상태인지 확인
- 재활성화 로직 재사용

---

## 🚨 중요 주의사항

### 1. 기존 데이터 호환성
```sql
-- 기존 데이터는 자동으로 NULL로 설정됨
-- 별도 마이그레이션 불필요
ALTER TABLE group_matching_field 
ADD COLUMN obsoleted_at TIMESTAMP NULL,
ADD COLUMN obsolete_reason VARCHAR(1000) NULL;
```

### 2. 쿼리 메서드 선택
| 상황 | 사용할 메서드 |
|------|-------------|
| 이름 중복 체크 (활성만) | `existsByNameAndObsoletedAtIsNull()` |
| 이름으로 찾기 (모든 상태) | `findByName()` |
| 목록 조회 (활성만) | `findAllByObsoletedAtIsNull()` |
| 전체 조회 (모든 상태) | `findAll()` |

### 3. 삭제 동작 변경
- **Before**: `deleteById()` → DB에서 완전 삭제
- **After**: `obsoletedAt = now()` → Soft delete
- **영향**: 복구 가능, 이력 보존

### 4. 재활성화 로직
- `obsoletedAt = null`
- `obsoleteReason = null`
- `createdAt = LocalDateTime.now()` ← 재설정!

---

## 📦 구현 우선순위

### Phase 1 (필수)
1. ✅ 엔티티 필드 추가
2. ✅ Repository 메서드 추가
3. 🔴 Service - addGroupMatchingField 수정
4. 🔴 Service - deleteGroupMatchingField 수정
5. 📝 테스트 코드 수정

### Phase 2 (고려)
6. ❓ 목록 조회 API (추가 시)
7. ❓ 폐기된 필드 목록 조회 API (관리용)
8. ❓ 수동 재활성화 API

---

## 🔍 체크리스트

- [ ] 엔티티에 `obsoletedAt`, `obsoleteReason` 추가
- [ ] 엔티티 필드 `val` → `var` 변경
- [ ] Repository에 `findByName` 추가
- [ ] Repository에 `existsByNameAndObsoletedAtIsNull` 추가
- [ ] Repository에 `findAllByObsoletedAtIsNull` 추가 (목록 API용)
- [ ] Service - 재활성화 로직 구현
- [ ] Service - Soft delete로 변경
- [ ] 테스트 - 재활성화 시나리오 추가
- [ ] 테스트 - Soft delete 검증
- [ ] DB 마이그레이션 스크립트 작성 (필요 시)
