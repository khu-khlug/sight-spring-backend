# CLAUDE.local.md (로컬 전용)

> **이 파일은 로컬에서만 사용되며 GitHub에 공유되지 않습니다.**
>
> **마지막 업데이트**: 2025-11-27 04:39 KST  
> **기준 커밋**: `bb7349e` - feat: 관심분야 삭제 Controller 구현

## 🔀 Git 커밋 컨벤션

### 한 파일씩 커밋하기
- **원칙**: 여러 파일을 한 번에 커밋하지 않고, **한 파일씩** 개별 커밋
- **이유**: 변경사항 추적 용이, 리뷰 편의성, 롤백 간편

**잘못된 예**:
```bash
# ❌ 여러 파일을 한 번에 커밋
git add file1.kt file2.kt file3.kt
git commit -m "feat: 기능 추가"
```

**올바른 예**:
```bash
# ✅ DTO 파일
git add src/main/kotlin/com/sight/controllers/http/dto/GetFieldRequestsResponse.kt
git commit -m "feat: 관심분야 추가 요청 목록 Response-DTO 생성"

# ✅ Service 파일
git add src/main/kotlin/com/sight/service/GroupMatchingFieldRequestService.kt
git commit -m "feat: 관심분야 추가 요청 목록 Service 구현"

# ✅ Test 파일
git add src/test/kotlin/com/sight/service/GroupMatchingFieldRequestServiceTest.kt
git commit -m "test: 관심분야 추가 요청 목록 Service 테스트 작성"
```

### 커밋 순서
1. DTO → 2. Service → 3. Service Test → 4. Controller

### 스테이징 규칙
- **원칙**: `git add .` 사용 금지, **파일 경로를 명시적으로 지정**
- **이유**: 의도하지 않은 파일(아티팩트, 임시 파일 등) 커밋 방지

**잘못된 예**:
```bash
# ❌ git add . 사용 금지
git add .
git commit -m "feat: 기능 추가"
```

**올바른 예**:
```bash
# ✅ 파일 경로 명시
git add src/main/kotlin/com/sight/service/GroupMatchingFieldService.kt
git commit -m "feat: 관심분야 서비스 구현"

# ✅ 여러 파일을 한 커밋에 포함할 경우도 명시
git add src/main/kotlin/com/sight/repository/GroupMatchingFieldRepository.kt \
        src/main/kotlin/com/sight/service/GroupMatchingFieldService.kt \
        src/test/kotlin/com/sight/service/GroupMatchingFieldServiceTest.kt
git commit -m "refactor: obsolete 확인을 서비스에서 하도록 변경"
```


### 테스트 작성 규칙
- **Service Test**: 필수 작성
- **Controller Test**: 별도 지시가 없으면 작성하지 않음
  - Controller는 Service를 호출하는 얇은 레이어
  - Service Test로 충분히 커버됨

## 🎯 Validation 컨벤션

### Jakarta Validation 어노테이션
- `@NotBlank`: **null, 빈 문자열(""), 공백 문자열("   ") 모두 검증**
  - 문자열 필드의 필수 입력 검증에 사용
  - Controller에서 `@Valid`로 자동 검증
- `@NotNull`: null만 검증 (빈 문자열은 통과)
- `@Valid`: 중첩 객체 validation
- `@Size(min, max)`: 문자열 길이 또는 컬렉션 크기 검증
- `@Pattern(regexp)`: 정규식 패턴 검증

### 계층별 책임 분리

#### Controller 계층
```kotlin
@PostMapping("/items")
fun addItem(@Valid @RequestBody request: AddItemRequest): AddItemResponse {
    // @Valid가 자동으로 DTO validation 수행
    val item = itemService.addItem(request)
    return AddItemResponse(item.id, item.name)
}
```

#### Service 계층
```kotlin
fun addItem(request: AddItemRequest): Item {
    // ❌ 잘못된 예: @NotBlank가 이미 검증함
    // if (request.name.isBlank()) {
    //     throw UnprocessableEntityException("이름은 필수입니다")
    // }
    
    // ✅ 올바른 예: 비즈니스 로직 검증만 수행
    if (itemRepository.existsByName(request.name)) {
        throw UnprocessableEntityException("이미 존재하는 이름입니다")
    }
    
    // trim() 불필요 - @NotBlank가 이미 공백 검증
    return itemRepository.save(Item(
        id = UlidCreator.getUlid().toString(),
        name = request.name, // 그대로 사용
    ))
}
```

### 중복 검증 금지
- `@NotBlank` 적용 → Service에서 `isBlank()`, `isEmpty()`, `trim()` 체크 **불필요**
- `@NotNull` 적용 → Service에서 null 체크 **불필요**
- **원칙**: Controller validation은 형식 검증, Service는 비즈니스 로직 검증

### 테스트 작성 시 주의사항
```kotlin
// ❌ 불필요한 테스트 - @NotBlank가 이미 검증
@Test
fun `빈 문자열이면 예외를 던진다`() {
    val request = AddItemRequest(name = "")
    assertThrows<UnprocessableEntityException> {
        service.addItem(request)
    }
}

// ❌ 불필요한 테스트 - @NotBlank가 이미 검증
@Test
fun `공백 문자열이면 예외를 던진다`() {
    val request = AddItemRequest(name = "   ")
    assertThrows<UnprocessableEntityException> {
        service.addItem(request)
    }
}

// ✅ 필요한 테스트 - 비즈니스 로직 검증
@Test
fun `중복된 이름이면 예외를 던진다`() {
    val request = AddItemRequest(name = "백엔드")
    given(repository.existsByName("백엔드")).willReturn(true)
    
    assertThrows<UnprocessableEntityException> {
        service.addItem(request)
    }
}
```

## 🗂️ Soft Delete 패턴

### Soft Delete 사용 정책

#### Obsoleted 필드의 유효성 체크
**문맥**: 쿼리 파라미터나 필터링에 fieldId 사용 시

- **원칙**: Obsoleted된 필드는 **"유효하지 않음"**으로 간주
- **적용**: API 쿼리 파라미터 검증 시
  - `fieldId` 필터링: obsoleted된 필드면 에러 반환
  - 신규 데이터 입력: obsoleted된 필드 선택 불가

**예시**:
```kotlin
// ❌ Obsoleted된 필드로 필터링 시도
GET /group-matchings/123/answers?fieldId=obsoleted-field-id
→ 400 Bad Request: "유효하지 않은 관심분야입니다"

// ❌ Obsoleted된 필드 선택 시도
POST /group-matchings/123/answers
{ "fieldIds": ["obsoleted-field-id"] }
→ 400 Bad Request: "유효하지 않은 관심분야입니다"
```

#### Obsoleted 필드의 조회/응답
**문맥**: 과거 데이터 조회 시 (이전 그룹매칭 등)

- **원칙**: Obsoleted 상태와 **무관하게 그대로 조회/응답**
- **이유**: 과거 데이터의 무결성 보존 - soft delete를 사용하는 핵심 이유
- **적용**: 
  - `selectedFields` 응답: obsoleted된 fieldId도 포함
  - 과거 그룹매칭 조회: 당시 선택했던 필드 정보 그대로 표시

**예시**:
```kotlin
// ✅ 과거 응답 조회 시 obsoleted된 필드도 포함
GET /group-matchings/123/answers
→ {
    "selectedFields": ["active-field-id", "obsoleted-field-id"], // 모두 포함
    ...
  }
```

### 엔티티 설계
```kotlin
@Entity
data class Resource(
    @Id val id: String,
    @Column(name = "name") val name: String,
    @Column(name = "created_at") var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "obsoleted_at") var obsoletedAt: LocalDateTime? = null,
    @Column(name = "obsolete_reason") var obsoleteReason: String? = null,
)
```

### 재활성화 로직
```kotlin
fun addResource(request: AddResourceRequest): Resource {
    val existing = repository.findByName(request.name)
    
    // 활성 상태 중복
    if (existing != null && existing.obsoletedAt == null) {
        throw UnprocessableEntityException("이미 존재하는 리소스입니다")
    }
    
    // 폐기 상태 → 재활성화
    if (existing != null && existing.obsoletedAt != null) {
        existing.obsoletedAt = null
        existing.obsoleteReason = null
        existing.createdAt = LocalDateTime.now()
        return repository.save(existing)
    }
    
    // 새로 생성
    return repository.save(Resource(
        id = UlidCreator.getUlid().toString(),
        name = request.name,
    ))
}
```

### Soft Delete
```kotlin
fun deleteResource(id: String) {
    val resource = repository.findById(id)
        .orElseThrow { NotFoundException("리소스를 찾을 수 없습니다") }
    
    resource.obsoletedAt = LocalDateTime.now()
    resource.obsoleteReason = "운영진 삭제"
    
    repository.save(resource)
}
```

## 📝 추가 노트

- Validation 관련 중복 코드 발견 시 이 문서 참조
- Soft delete 패턴 적용 시 엔티티에 `var` 사용 필요 (JPA update)
- 재활성화 시 `createdAt` 업데이트 고려
