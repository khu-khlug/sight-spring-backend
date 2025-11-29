# 관심분야 추가 API 수정사항 구현 계획

## 🔍 변경사항 분석

### 기존 구현 (현재)
```kotlin
// GroupMatchingField 엔티티
- id: String
- name: String
- createdAt: LocalDateTime

// Service 로직
- 단순 중복 체크: existsByName()
- 중복 시 UnprocessableEntityException 발생
```

### 새로운 요구사항 (수정사항)
1. **Soft Delete 지원** - `obsoletedAt`, `obsoleteReason` 필드 추가
2. **재활성화 로직** - obsoleted 상태인 필드를 다시 활성화
3. **Validation 강화** - 빈 문자열, 공백 문자 체크

## 📋 주요 변경사항

### 1. 엔티티 필드 추가
- `obsoletedAt: LocalDateTime?` - 폐기된 시각
- `obsoleteReason: String?` - 폐기 사유

### 2. 비즈니스 로직 변경
**기존**: 이름 중복 시 무조건 에러  
**신규**: 
- 활성 상태 중복 → 에러
- 폐기 상태 중복 → 재활성화

### 3. Validation
- `@NotBlank` 어노테이션이 빈 문자열, 공백 문자 검증을 모두 수행
- Service 계층에서 별도 검증 불필요

## 📁 수정/생성할 파일 목록

### 1. 엔티티 수정
**파일**: `src/main/kotlin/com/sight/domain/groupmatching/GroupMatchingField.kt`

**변경 전**:
```kotlin
data class GroupMatchingField(
    @Id val id: String,
    @Column(name = "name") val name: String,
    @CreationTimestamp val createdAt: LocalDateTime = LocalDateTime.now(),
)
```

**변경 후**:
```kotlin
data class GroupMatchingField(
    @Id val id: String,
    @Column(name = "name") val name: String,
    @CreationTimestamp @Column(name = "created_at") var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "obsoleted_at") var obsoletedAt: LocalDateTime? = null,
    @Column(name = "obsolete_reason") var obsoleteReason: String? = null,
)
```

**주의**: `createdAt`, `obsoletedAt`, `obsoleteReason`을 `var`로 변경 (재활성화 시 수정 필요)

### 2. Repository 수정
**파일**: `src/main/kotlin/com/sight/repository/GroupMatchingFieldRepository.kt`

**추가할 메서드**:
```kotlin
interface GroupMatchingFieldRepository : JpaRepository<GroupMatchingField, String> {
    fun existsByName(name: String): Boolean
    
    // 신규 추가
    fun findByName(name: String): GroupMatchingField?
    fun existsByNameAndObsoletedAtIsNull(name: String): Boolean
}
```

### 3. Request DTO 수정
**파일**: `src/main/kotlin/com/sight/controllers/http/dto/AddGroupMatchingFieldRequest.kt`

**현재 상태** (이미 적용됨):
```kotlin
data class AddGroupMatchingFieldRequest(
    @field:NotBlank(message = "관심분야 이름은 필수입니다")
    @field:JsonProperty("fieldName")
    val fieldName: String,
)
```

✅ 이미 `@NotBlank`가 적용되어 있어 수정 불필요

### 4. Service 수정
**파일**: `src/main/kotlin/com/sight/service/GroupMatchingFieldService.kt`

**변경 전**:
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

**변경 후**:
```kotlin
fun addGroupMatchingField(request: AddGroupMatchingFieldRequest): GroupMatchingField {
    // @NotBlank가 이미 빈 문자열/공백 검증을 수행하므로 여기서는 불필요
    
    // 1. 기존 필드 확인
    val existingField = groupMatchingFieldRepository.findByName(request.fieldName)
    
    // 2-1. 활성 상태 중복 체크
    if (existingField != null && existingField.obsoletedAt == null) {
        throw UnprocessableEntityException("이미 존재하는 관심분야 이름입니다")
    }
    
    // 2-2. 폐기 상태 -> 재활성화
    if (existingField != null && existingField.obsoletedAt != null) {
        existingField.obsoletedAt = null
        existingField.obsoleteReason = null
        existingField.createdAt = LocalDateTime.now()
        return groupMatchingFieldRepository.save(existingField)
    }
    
    // 2-3. 새로 생성
    val field = GroupMatchingField(
        id = UlidCreator.getUlid().toString(),
        name = request.fieldName,
    )
    
    return groupMatchingFieldRepository.save(field)
}
```

### 5. Service Test 수정
**파일**: `src/test/kotlin/com/sight/service/GroupMatchingFieldServiceTest.kt`

**추가할 테스트**:
```kotlin
// @NotBlank가 빈 문자열/공백을 검증하므로 별도 테스트 불필요

@Test
fun `addGroupMatchingField는 폐기된 필드를 재활성화한다`() {
    // given
    val request = AddGroupMatchingFieldRequest(fieldName = "백엔드")
    val obsoletedField = GroupMatchingField(
        id = "field-1",
        name = "백엔드",
        createdAt = LocalDateTime.of(2024, 1, 1, 0, 0),
        obsoletedAt = LocalDateTime.of(2024, 6, 1, 0, 0),
        obsoleteReason = "사용 안 함",
    )
    
    given(groupMatchingFieldRepository.findByName("백엔드")).willReturn(obsoletedField)
    given(groupMatchingFieldRepository.save(any<GroupMatchingField>())).willAnswer { it.arguments[0] }
    
    // when
    val result = groupMatchingFieldService.addGroupMatchingField(request)
    
    // then
    assertNull(result.obsoletedAt)
    assertNull(result.obsoleteReason)
    assertTrue(result.createdAt.isAfter(LocalDateTime.of(2024, 1, 1, 0, 0)))
    verify(groupMatchingFieldRepository).save(obsoletedField)
}

@Test
fun `addGroupMatchingField는 활성 상태 중복 시 UnprocessableEntityException을 던진다`() {
    // given
    val request = AddGroupMatchingFieldRequest(fieldName = "백엔드")
    val activeField = GroupMatchingField(
        id = "field-1",
        name = "백엔드",
        obsoletedAt = null,
    )
    
    given(groupMatchingFieldRepository.findByName("백엔드")).willReturn(activeField)
    
    // when & then
    assertThrows<UnprocessableEntityException> {
        groupMatchingFieldService.addGroupMatchingField(request)
    }
}
```

### 6. Delete Service 수정
**파일**: `src/main/kotlin/com/sight/service/GroupMatchingFieldService.kt`

**Soft Delete 구현**:
```kotlin
fun deleteGroupMatchingField(fieldId: String) {
    val field = groupMatchingFieldRepository.findById(fieldId)
        .orElseThrow { NotFoundException("존재하지 않는 관심분야입니다") }
    
    // Hard delete 대신 Soft delete
    field.obsoletedAt = LocalDateTime.now()
    field.obsoleteReason = "운영진 삭제"
    
    groupMatchingFieldRepository.save(field)
}
```

## 📦 구현 순서

1. **엔티티 수정** - obsoletedAt, obsoleteReason 필드 추가
2. **Repository 수정** - findByName, existsByNameAndObsoletedAtIsNull 메서드 추가
3. **Request DTO 수정** - @NotBlank validation 추가 //필요없다며?
4. **Service 수정** - 재활성화 로직, 공백 검증 추가
5. **Delete Service 수정** - Hard delete → Soft delete 변경
6. **Service Test 수정** - 새로운 시나리오 테스트 추가
7. **테스트 실행** (`./gradlew test`)
8. **빌드 검증** (`./gradlew build`)
9. **DB 마이그레이션 스크립트 작성** (필요 시)
10. **커밋 및 푸시**

## 🔍 주요 체크포인트

### DB 마이그레이션
```sql
ALTER TABLE group_matching_field 
ADD COLUMN obsoleted_at TIMESTAMP NULL,
ADD COLUMN obsolete_reason VARCHAR(1000) NULL;
```

### Validation 순서
1. `@NotBlank` - Controller에서 자동 검증 (빈 문자열/공백)
2. Service - 활성 상태 중복 체크
3. 폐기 상태 중복 → 재활성화

### 재활성화 시 업데이트 필드
- `obsoletedAt` → `null`
- `obsoleteReason` → `null`
- `createdAt` → `LocalDateTime.now()`

## ⚠️ 주의사항

1. **엔티티 data class의 var 사용**
   - 재활성화를 위해 `createdAt`, `obsoletedAt`, `obsoleteReason`는 `var`로 변경 필요
   - JPA 업데이트를 위해 mutable 필드 필요

2. **기존 데이터 호환성**
   - 기존 데이터는 `obsoletedAt = null`, `obsoleteReason = null` 상태
   - 새 필드는 nullable이므로 기존 데이터와 호환됨

3. **Delete 로직 변경**
   - Hard delete → Soft delete로 변경
   - 기존 `deleteById()` 호출을 `save()` + 필드 업데이트로 변경
