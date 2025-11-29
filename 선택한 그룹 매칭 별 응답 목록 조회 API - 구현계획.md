# 선택한 그룹 매칭 별 응답 목록 조회 API - 구현 계획

## 📋 목표

`GET /group-matchings/:groupMatchingId/answers` API 구현
- 운영진 권한 필수
- 페이지네이션 지원
- 필터링: `groupType`, `fieldId`
- 정렬: 생성일시 내림차순

## ⚠️ User Review Required

> [!WARNING]
> **fieldId 필터링 기능은 Soft Delete 스키마 확정 후 구현**
> 
> - `GroupMatchingField`에 `obsoletedAt` 컬럼 추가 필요
> - Phase 1, 2에서는 TODO 주석으로 표시
> - Phase 3에서 스키마 확정 후 구현

## 📦 구현 단계

### Phase 1: 기본 기능 구현

**목표**: fieldId 필터링 제외한 핵심 기능

#### 1-1. Response DTO 생성

##### [NEW] [GetAnswersResponse.kt](file:///c:/Users/nananina/Documents/01_KHLUG/sight-spring-backend/src/main/kotlin/com/sight/controllers/http/dto/GetAnswersResponse.kt)
```kotlin
data class GetAnswersResponse(
    val answers: List<AnswerDto>,
    val total: Int,
)

data class AnswerDto(
    val answerId: String,
    val answerUserId: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val groupType: String, // "STUDY" | "PROJECT"
    val isPreferOnline: Boolean,
    val selectedFields: List<String>, // fieldId 목록
    val subjectIdeas: List<String>, // subject 목록
    val matchedGroupIds: List<Long>,
)
```

#### 1-2. Repository 메서드 추가

##### [MODIFY] [GroupMatchingAnswerRepository.kt](file:///c:/Users/nananina/Documents/01_KHLUG/sight-spring-backend/src/main/kotlin/com/sight/repository/GroupMatchingAnswerRepository.kt)
```kotlin
// 그룹매칭별 응답 조회 (내림차순)
fun findAllByGroupMatchingIdOrderByCreatedAtDesc(
    groupMatchingId: String
): List<GroupMatchingAnswer>
```

필요한 추가 Repository:
- `GroupMatchingAnswerFieldRepository` - selectedFields 조회
- `GroupMatchingSubjectRepository` - subjectIdeas 조회
- `MatchedGroupRepository` - matchedGroupIds 조회

#### 1-3. Service 구현

##### [NEW] [GroupMatchingAnswerService.kt](file:///c:/Users/nananina/Documents/01_KHLUG/sight-spring-backend/src/main/kotlin/com/sight/service/GroupMatchingAnswerService.kt)
```kotlin
@Service
class GroupMatchingAnswerService(
    private val answerRepository: GroupMatchingAnswerRepository,
    private val answerFieldRepository: GroupMatchingAnswerFieldRepository,
    private val subjectRepository: GroupMatchingSubjectRepository,
    private val matchedGroupRepository: MatchedGroupRepository,
) {
    fun getAnswers(
        groupMatchingId: String,
        // Phase 2에서 추가: groupType, offset, limit
        // Phase 3에서 추가: fieldId
    ): GetAnswersResponse {
        // 1. 응답 목록 조회 (내림차순)
        val answers = answerRepository
            .findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId)
        
        // 2. 각 응답에 대해 연관 데이터 조회
        val answerDtos = answers.map { answer ->
            AnswerDto(
                answerId = answer.id,
                answerUserId = answer.userId,
                createdAt = answer.createdAt,
                updatedAt = answer.updatedAt,
                groupType = answer.groupType.name,
                isPreferOnline = answer.isPreferOnline,
                selectedFields = getSelectedFields(answer.id),
                subjectIdeas = getSubjectIdeas(answer.id),
                matchedGroupIds = getMatchedGroupIds(answer.id),
            )
        }
        
        return GetAnswersResponse(
            answers = answerDtos,
            total = answerDtos.size,
        )
    }
    
    private fun getSelectedFields(answerId: String): List<String> {
        return answerFieldRepository.findAllByAnswerId(answerId)
            .map { it.fieldId }
    }
    
    private fun getSubjectIdeas(answerId: String): List<String> {
        return subjectRepository.findAllByAnswerId(answerId)
            .map { it.subject }
    }
    
    private fun getMatchedGroupIds(answerId: String): List<Long> {
        return matchedGroupRepository.findAllByAnswerId(answerId)
            .map { it.groupId }
    }
}
```

#### 1-4. Controller 구현

##### [NEW] [GroupMatchingAnswerController.kt](file:///c:/Users/nananina/Documents/01_KHLUG/sight-spring-backend/src/main/kotlin/com/sight/controllers/http/GroupMatchingAnswerController.kt)
```kotlin
@RestController
class GroupMatchingAnswerController(
    private val answerService: GroupMatchingAnswerService,
) {
    @Auth([UserRole.MANAGER])
    @GetMapping("/group-matchings/{groupMatchingId}/answers")
    fun getAnswers(
        @PathVariable groupMatchingId: String,
        // Phase 2에서 추가: @RequestParam 파라미터들
    ): GetAnswersResponse {
        return answerService.getAnswers(groupMatchingId)
    }
}
```

#### 1-5. Service Test 작성

##### [NEW] [GroupMatchingAnswerServiceTest.kt](file:///c:/Users/nananina/Documents/01_KHLUG/sight-spring-backend/src/test/kotlin/com/sight/service/GroupMatchingAnswerServiceTest.kt)

테스트 시나리오:
- 응답이 없으면 빈 배열 반환
- 생성일시 기준 내림차순 정렬
- selectedFields 없으면 빈 배열
- subjectIdeas 없으면 빈 배열
- matchedGroupIds 없으면 빈 배열

---

### Phase 2: 쿼리스트링 구현

**목표**: groupType 필터링, 페이지네이션

#### 2-1. DTO 수정

##### [MODIFY] [GetAnswersResponse.kt](file:///c:/Users/nananina/Documents/01_KHLUG/sight-spring-backend/src/main/kotlin/com/sight/controllers/http/dto/GetAnswersResponse.kt)
```kotlin
data class GetAnswersResponse(
    val answers: List<AnswerDto>,
    val total: Int,
    val hasNext: Boolean, // 추가
)
```

#### 2-2. Service 수정 - 필터링 & 페이지네이션

##### [MODIFY] [GroupMatchingAnswerService.kt](file:///c:/Users/nananina/Documents/01_KHLUG/sight-spring-backend/src/main/kotlin/com/sight/service/GroupMatchingAnswerService.kt)
```kotlin
fun getAnswers(
    groupMatchingId: String,
    groupType: String?, // 추가
    offset: Int = DEFAULT_OFFSET, // 추가
    limit: Int = DEFAULT_LIMIT, // 추가
): GetAnswersResponse {
    // 1. groupType 검증
    if (groupType != null && groupType !in listOf("STUDY", "PROJECT")) {
        throw BadRequestException("유효하지 않은 그룹 타입입니다")
    }
    
    // 2. offset/limit 검증
    if (offset < 0) {
        throw BadRequestException("offset은 0 이상이어야 합니다")
    }
    if (limit <= 0) {
        throw BadRequestException("limit은 양의 정수여야 합니다")
    }
    
    // 3. 응답 조회 & 필터링
    var answers = answerRepository
        .findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId)
    
    // groupType 필터링
    if (groupType != null) {
        answers = answers.filter { it.groupType.name == groupType }
    }
    
    val total = answers.size
    
    // 4. offset 범위 검증
    if (offset >= total && total > 0) {
        throw BadRequestException("offset이 범위를 벗어났습니다")
    }
    
    // 5. 페이지네이션
    val pagedAnswers = answers
        .drop(offset)
        .take(limit)
    
    // 6. DTO 변환
    val answerDtos = pagedAnswers.map { /* ... */ }
    
    return GetAnswersResponse(
        answers = answerDtos,
        total = total,
        hasNext = offset + limit < total,
    )
}
```

#### 2-3. Controller 수정

##### [MODIFY] [GroupMatchingAnswerController.kt](file:///c:/Users/nananina/Documents/01_KHLUG/sight-spring-backend/src/main/kotlin/com/sight/controllers/http/GroupMatchingAnswerController.kt)
```kotlin
@GetMapping("/group-matchings/{groupMatchingId}/answers")
fun getAnswers(
    @PathVariable groupMatchingId: String,
    @RequestParam(required = false) groupType: String?,
    @RequestParam(required = false, defaultValue = "0") offset: Int,
    @RequestParam(required = false, defaultValue = "20") limit: Int,
): GetAnswersResponse {
    return answerService.getAnswers(groupMatchingId, groupType, offset, limit)
}
```

#### 2-4. Service Test 추가

테스트 시나리오:
- groupType 값이 유효하지 않으면 에러
- groupType 필터링 동작 확인
- offset/limit 검증
- 페이지네이션 동작 확인
- hasNext 계산 확인

---

### Phase 3: fieldId 필터링 (Obsolete 스키마 확정 후)

> [!IMPORTANT]
> **선행 조건**: `GroupMatchingField`에 `obsoletedAt` 컬럼 추가

#### 3-1. Repository 메서드 추가

##### [MODIFY] [GroupMatchingFieldRepository.kt](file:///c:/Users/nananina/Documents/01_KHLUG/sight-spring-backend/src/main/kotlin/com/sight/repository/GroupMatchingFieldRepository.kt)
```kotlin
// 활성 상태 필드만 존재 여부 확인
fun existsByIdAndObsoletedAtIsNull(id: String): Boolean
```

#### 3-2. Service 수정

##### [MODIFY] [GroupMatchingAnswerService.kt](file:///c:/Users/nananina/Documents/01_KHLUG/sight-spring-backend/src/main/kotlin/com/sight/service/GroupMatchingAnswerService.kt)
```kotlin
fun getAnswers(
    groupMatchingId: String,
    groupType: String?,
    fieldId: String?, // 추가
    offset: Int = DEFAULT_OFFSET,
    limit: Int = DEFAULT_LIMIT,
): GetAnswersResponse {
    // TODO: Phase 3 - fieldId 필터링 구현
    // if (fieldId != null) {
    //     // 1. 유효성 검증 (obsoleted 필드는 유효하지 않음)
    //     if (!fieldRepository.existsByIdAndObsoletedAtIsNull(fieldId)) {
    //         throw BadRequestException("유효하지 않은 관심분야입니다")
    //     }
    //     
    //     // 2. 해당 fieldId를 가진 응답만 필터링
    //     val answerIds = answerFieldRepository
    //         .findAllByFieldId(fieldId)
    //         .map { it.answerId }
    //     answers = answers.filter { it.id in answerIds }
    // }
    
    // ... 나머지 로직
}
```

#### 3-3. Controller 수정

##### [MODIFY] [GroupMatchingAnswerController.kt](file:///c:/Users/nananina/Documents/01_KHLUG/sight-spring-backend/src/main/kotlin/com/sight/controllers/http/GroupMatchingAnswerController.kt)
```kotlin
@GetMapping("/group-matchings/{groupMatchingId}/answers")
fun getAnswers(
    @PathVariable groupMatchingId: String,
    @RequestParam(required = false) groupType: String?,
    @RequestParam(required = false) fieldId: String?, // 추가
    @RequestParam(required = false, defaultValue = "0") offset: Int,
    @RequestParam(required = false, defaultValue = "20") limit: Int,
): GetAnswersResponse {
    return answerService.getAnswers(groupMatchingId, groupType, fieldId, offset, limit)
}
```

#### 3-4. Service Test 추가

테스트 시나리오:
- fieldId가 유효하지 않으면 에러
- fieldId 필터링 동작 확인
- fieldId + groupType 복합 필터링 확인

---

## 📝 체크리스트

### Phase 1: 기본 기능
- [ ] Response DTO 생성
- [ ] Repository 메서드 추가
- [ ] Service 구현 (기본 조회)
- [ ] Controller 구현
- [ ] Service Test (5개 시나리오)
- [ ] 빌드 & 테스트
- [ ] 커밋 & 푸시

### Phase 2: 쿼리스트링
- [ ] DTO 수정
- [ ] Service 수정 (필터링 & 페이지네이션)
- [ ] Controller 수정 (쿼리 파라미터)
- [ ] Service Test 추가 (6개 시나리오)
- [ ] 빌드 & 테스트
- [ ] 커밋 & 푸시

### Phase 3: fieldId 필터링
- [ ] 스키마 확정 대기
- [ ] Repository 메서드 추가
- [ ] Service TODO 주석 구현
- [ ] Controller fieldId 파라미터 활성화
- [ ] Service Test 추가 (3개 시나리오)
- [ ] 빌드 & 테스트
- [ ] 커밋 & PR 생성

## 🔧 Verification Plan

### Automated Tests
```bash
./gradlew test --tests "GroupMatchingAnswerServiceTest"
./gradlew build
```

### Manual Verification
- Phase 1: Postman으로 기본 조회 테스트
- Phase 2: groupType 필터링, 페이지네이션 테스트
- Phase 3: fieldId 필터링 테스트 (스키마 확정 후)
