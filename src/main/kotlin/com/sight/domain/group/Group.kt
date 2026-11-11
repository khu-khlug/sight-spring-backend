package com.sight.domain.group

import com.sight.core.groupauth.GroupVisibility
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

// JPQL 엔티티 이름 Group은 예약어 GROUP과 겹치므로 KhlugGroup을 쓴다.
@Entity(name = "KhlugGroup")
// TODO: 그룹 목록 조회 API를 변경할 때 필터·정렬에 필요한 인덱스를 추가한다.
@Table(name = "khlug_group")
class Group(
    id: Long,
    title: String,
    description: String?,
    category: GroupCategory,
    interests: List<GroupInterest>?,
    techStack: String?,
    repositoryUrls: String?,
    allowJoin: Boolean,
    visibility: GroupVisibility,
    authorMemberId: Long,
    createdAt: Instant,
) {
    // 그룹 식별자.
    @Id
    @Column(name = "id", nullable = false)
    val id: Long = id

    // 그룹 이름. HTML로 해석하지 않는 일반 텍스트이며 제어 문자를 허용하지 않는다.
    @Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
    var title: String = title
        private set

    // 그룹 설명. 일반 텍스트이며 제어 문자 중 줄바꿈만 허용한다.
    @Column(name = "description", columnDefinition = "LONGTEXT")
    var description: String? = description
        private set

    // 그룹 유형. 운영 유형이면 운영진도 정보를 변경할 수 있고, 내보내기·탈퇴 규칙이 달라진다.
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    var category: GroupCategory = category
        private set

    // 진행 상태. 종료·중단 여부에 따라 변경 API의 허용 여부가 정해진다.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: GroupStatus = GroupStatus.PROGRESS
        private set

    // 관심 분야 enum 이름을 쉼표로 이은 문자열. null과 빈 목록(빈 문자열)을 구분해 저장한다.
    @Column(name = "interests", length = 1023)
    var interestNames: String? = toInterestNames(interests)
        private set

    // 쉼표로 구분한 사용 기술. 사용 기술이 없으면 null이다.
    @Column(name = "tech_stack", length = JOINED_TEXT_MAX_LENGTH)
    var techStack: String? = null
        private set

    // LF 줄바꿈으로 구분한 저장소 URL. URL 안의 쉼표·퍼센트 인코딩은 원문 그대로 둔다. 저장소가 없으면 null이다.
    @Column(name = "repository_urls", length = JOINED_TEXT_MAX_LENGTH)
    var repositoryUrls: String? = null
        private set

    // 참여 신청 가능 여부. 꺼져 있으면 참가를 거부한다.
    @Column(name = "allow_join", nullable = false)
    var allowJoin: Boolean = allowJoin
        private set

    // 공개 범위. 열람 권한을 판단하는 기준이다.
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    var visibility: GroupVisibility = visibility
        private set

    // 그룹을 만든 회원.
    @Column(name = "author_member_id", nullable = false)
    val authorMemberId: Long = authorMemberId

    // 현재 그룹장. 운영 그룹에서 마지막 그룹원인 그룹장이 나가면 null이다.
    @Column(name = "leader_member_id")
    var leaderMemberId: Long? = authorMemberId
        private set

    // 그룹 포트폴리오 발행 여부. 카드·기록의 포트폴리오 포함 여부와는 별개다.
    @Column(name = "is_portfolio_published", nullable = false)
    var isPortfolioPublished: Boolean = false
        private set

    // 현재 그룹원 수.
    @Column(name = "member_count", nullable = false)
    var memberCount: Long = 0
        private set

    // 현재 리스트 수.
    @Column(name = "list_count", nullable = false)
    var listCount: Long = 0
        private set

    // 삭제되지 않은 카드 수. 비활성 카드를 포함한다.
    @Column(name = "card_count", nullable = false)
    var cardCount: Long = 0
        private set

    // 삭제되지 않은 카드의 삭제되지 않은 기록 수.
    @Column(name = "record_count", nullable = false)
    var recordCount: Long = 0
        private set

    // 그룹 생성 일시.
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = createdAt

    // 최근 활동 일시. 각 API 명세가 활동으로 인정한 변경에서만 갱신한다.
    @Column(name = "last_activity_at", nullable = false)
    var lastActivityAt: Instant = createdAt
        private set

    init {
        require(id > 0) { "식별자가 올바르지 않습니다." }
        require(authorMemberId > 0) { "회원 식별자가 올바르지 않습니다." }
        validateInfo(title, description, category, interests, visibility)
        this.title = title.trim()
        this.techStack = normalizeTechStack(techStack)
        this.repositoryUrls = normalizeRepositoryUrls(repositoryUrls)
    }

    // 관심 분야 목록. 저장값이 null이면 null, 빈 문자열이면 빈 목록이다.
    val interests: List<GroupInterest>?
        get() = fromInterestNames(interestNames)

    val isEnded: Boolean
        get() = status.isEnded

    val isStopped: Boolean
        get() = status == GroupStatus.STOP

    // 쉼표로 나눈 사용 기술 목록. 저장값이 null이면 null이다.
    val techStackNames: List<String>?
        get() = techStack?.split(TECH_STACK_SEPARATOR)

    // LF로 나눈 저장소 URL 목록. 저장값이 null이면 null이다.
    val repositoryUrlList: List<String>?
        get() = repositoryUrls?.split(REPOSITORY_URL_SEPARATOR)

    fun isLeader(memberId: Long): Boolean = leaderMemberId == memberId

    fun hasRepositoryUrl(url: String): Boolean = repositoryUrlList?.contains(url) == true

    // 정보 전체를 덮어쓴다. 진행 중인 그룹에서만 바꿀 수 있으며 실제로 바뀐 항목을 반환한다.
    fun updateInfo(
        title: String,
        description: String?,
        category: GroupCategory,
        interests: List<GroupInterest>?,
        techStack: String?,
        repositoryUrls: String?,
        allowJoin: Boolean,
        visibility: GroupVisibility,
    ): Set<GroupInfoItem> {
        require(status == GroupStatus.PROGRESS) { "진행 중인 그룹만 정보를 변경할 수 있습니다." }
        validateInfo(title, description, category, interests, visibility)
        val newTitle = title.trim()
        val newTechStack = normalizeTechStack(techStack)
        val newRepositoryUrls = normalizeRepositoryUrls(repositoryUrls)

        val changed = mutableSetOf<GroupInfoItem>()
        if (this.title != newTitle) changed += GroupInfoItem.TITLE
        if (this.description != description) changed += GroupInfoItem.DESCRIPTION
        if (this.category != category) changed += GroupInfoItem.CATEGORY
        if (this.interests != interests) changed += GroupInfoItem.INTERESTS
        if (this.techStack != newTechStack) changed += GroupInfoItem.TECH_STACK
        if (this.repositoryUrls != newRepositoryUrls) changed += GroupInfoItem.REPOSITORY_URLS
        if (this.allowJoin != allowJoin) changed += GroupInfoItem.ALLOW_JOIN
        if (this.visibility != visibility) changed += GroupInfoItem.VISIBILITY

        this.title = newTitle
        this.description = description
        this.category = category
        this.interestNames = toInterestNames(interests)
        this.techStack = newTechStack
        this.repositoryUrls = newRepositoryUrls
        this.allowJoin = allowJoin
        this.visibility = visibility
        return changed
    }

    // 진행 중인 그룹을 성공 또는 실패로 종료한다.
    fun end(result: GroupStatus) {
        require(result.isEnded) { "종료 상태는 성공 또는 실패여야 합니다." }
        require(status == GroupStatus.PROGRESS) { "진행 중인 그룹만 종료할 수 있습니다." }
        status = result
    }

    // 종료·중단된 그룹을 명시적으로 다시 진행한다. 경험치 회수에 쓰도록 변경 전 상태를 반환한다.
    fun resumeProgress(at: Instant): GroupStatus {
        require(status != GroupStatus.PROGRESS) { "이미 진행 중인 그룹입니다." }
        val previous = status
        status = GroupStatus.PROGRESS
        recordActivity(at)
        return previous
    }

    // 진행 중인 그룹을 중단한다.
    fun stop() {
        require(status == GroupStatus.PROGRESS) { "진행 중인 그룹만 중단할 수 있습니다." }
        status = GroupStatus.STOP
    }

    // 중단 해제를 허용한 활동이 실제로 일어났을 때 호출한다. 중단 상태였으면 진행 중으로 바꾸고 true를 반환한다.
    // 종료 상태는 되돌리지 않는다.
    fun resumeByActivity(): Boolean {
        if (status != GroupStatus.STOP) return false
        status = GroupStatus.PROGRESS
        return true
    }

    // 최근 활동 일시를 갱신한다. 동시 요청의 순서가 뒤바뀌어도 더 늦은 시각을 유지한다.
    fun recordActivity(at: Instant) {
        if (at.isAfter(lastActivityAt)) lastActivityAt = at
    }

    // 그룹장을 바꾼다. 같은 회원이면 false를 반환한다.
    fun changeLeader(memberId: Long?): Boolean {
        require(memberId == null || memberId > 0) { "회원 식별자가 올바르지 않습니다." }
        if (leaderMemberId == memberId) return false
        leaderMemberId = memberId
        return true
    }

    fun publishPortfolio() {
        require(!isPortfolioPublished) { "이미 포트폴리오를 발행한 그룹입니다." }
        isPortfolioPublished = true
    }

    fun cancelPortfolio() {
        require(isPortfolioPublished) { "포트폴리오를 발행하지 않은 그룹입니다." }
        isPortfolioPublished = false
    }

    fun increaseMemberCount() {
        memberCount += 1
    }

    fun decreaseMemberCount() {
        require(memberCount > 0) { "그룹원 수는 음수가 될 수 없습니다." }
        memberCount -= 1
    }

    fun increaseListCount() {
        listCount += 1
    }

    fun decreaseListCount() {
        require(listCount > 0) { "리스트 수는 음수가 될 수 없습니다." }
        listCount -= 1
    }

    fun increaseCardCount() {
        cardCount += 1
    }

    fun decreaseCardCount() {
        require(cardCount > 0) { "카드 수는 음수가 될 수 없습니다." }
        cardCount -= 1
    }

    // 카드 삭제·복구처럼 여러 기록이 한꺼번에 조회 대상에 들어오거나 빠질 때 개수를 함께 받는다.
    fun increaseRecordCount(count: Long = 1) {
        require(count >= 0) { "증가할 기록 수가 올바르지 않습니다." }
        recordCount += count
    }

    fun decreaseRecordCount(count: Long = 1) {
        require(count in 0..recordCount) { "기록 수는 음수가 될 수 없습니다." }
        recordCount -= count
    }

    companion object {
        const val TITLE_MAX_LENGTH: Int = 255
        const val JOINED_TEXT_MAX_LENGTH: Int = 255
        const val REPOSITORY_URL_MAX_COUNT: Int = 5
        const val TECH_STACK_SEPARATOR: String = ","
        const val REPOSITORY_URL_SEPARATOR: String = "\n"

        private val HTTP_URL_PATTERN = Regex("^https?://\\S+$", RegexOption.IGNORE_CASE)

        private const val INTEREST_SEPARATOR: String = ","

        private fun toInterestNames(interests: List<GroupInterest>?): String? = interests?.joinToString(INTEREST_SEPARATOR) { it.name }

        // 저장된 값에 허용하지 않는 이름이 있으면 데이터 오류이므로 예외를 던진다.
        private fun fromInterestNames(interestNames: String?): List<GroupInterest>? {
            if (interestNames == null) return null
            if (interestNames.isEmpty()) return emptyList()
            return interestNames.split(INTEREST_SEPARATOR).map { name ->
                requireNotNull(GroupInterest.entries.find { it.name == name }) { "허용하지 않는 관심 분야입니다: $name" }
            }
        }

        private fun String.codePointLength(): Int = codePointCount(0, length)

        private fun validateInfo(
            title: String,
            description: String?,
            category: GroupCategory,
            interests: List<GroupInterest>?,
            visibility: GroupVisibility,
        ) {
            val trimmedTitle = title.trim()
            require(trimmedTitle.isNotEmpty()) { "그룹 이름이 필요합니다." }
            require(trimmedTitle.codePointLength() <= TITLE_MAX_LENGTH) { "그룹 이름은 ${TITLE_MAX_LENGTH}자 이하여야 합니다." }
            require(trimmedTitle.none { it.isISOControl() }) { "그룹 이름에 제어 문자를 쓸 수 없습니다." }
            require(description == null || description.none { it.isISOControl() && it != '\n' }) {
                "그룹 설명에는 줄바꿈 외의 제어 문자를 쓸 수 없습니다."
            }
            require(interests == null || interests.size == interests.toSet().size) { "관심 분야에 중복 값이 있습니다." }
            require(visibility != GroupVisibility.MANAGER_ONLY || category == GroupCategory.MANAGE) {
                "운영진만 공개는 운영 그룹에서만 지정할 수 있습니다."
            }
        }

        // 빈 문자열과 null은 사용 기술 없음(null)으로 저장한다.
        private fun normalizeTechStack(techStack: String?): String? {
            if (techStack.isNullOrEmpty()) return null
            require(techStack.codePointLength() <= JOINED_TEXT_MAX_LENGTH) {
                "사용 기술은 ${JOINED_TEXT_MAX_LENGTH}자 이하여야 합니다."
            }
            val names = techStack.split(TECH_STACK_SEPARATOR)
            require(names.none { it.isBlank() }) { "사용 기술에 빈 이름이 있습니다." }
            require(names.size == names.toSet().size) { "사용 기술에 중복 값이 있습니다." }
            require(techStack.none { it.isISOControl() }) { "사용 기술에 제어 문자를 쓸 수 없습니다." }
            return techStack
        }

        // 빈 문자열과 null은 저장소 없음(null)으로 저장한다. URL 디코딩 없이 LF로 먼저 나눈다.
        private fun normalizeRepositoryUrls(repositoryUrls: String?): String? {
            if (repositoryUrls.isNullOrEmpty()) return null
            require(repositoryUrls.codePointLength() <= JOINED_TEXT_MAX_LENGTH) {
                "저장소 URL은 ${JOINED_TEXT_MAX_LENGTH}자 이하여야 합니다."
            }
            val urls = repositoryUrls.split(REPOSITORY_URL_SEPARATOR)
            require(urls.size <= REPOSITORY_URL_MAX_COUNT) { "저장소는 최대 ${REPOSITORY_URL_MAX_COUNT}개까지 등록할 수 있습니다." }
            require(urls.none { it.isEmpty() }) { "저장소 URL에 빈 값이 있습니다." }
            require(urls.size == urls.toSet().size) { "저장소 URL에 중복 값이 있습니다." }
            require(urls.all { HTTP_URL_PATTERN.matches(it) }) { "저장소 URL은 http 또는 https 주소여야 합니다." }
            return repositoryUrls
        }
    }
}
