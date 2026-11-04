package com.sight.domain.github

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Duration
import java.time.Instant

@Entity
// 인덱스 없음: 기본 키가 캐시 키이고, 오래된 캐시 교체는 최대 1,000행이라 전체 조회로 충분하다.
@Table(name = "github_cache")
class GithubCache(
    id: String,
    type: GithubCacheType,
    requestUrl: String,
    content: String,
    cachedAt: Instant,
    etag: String? = null,
) {
    // 공용 토큰 조회 캐시의 고유 식별자. 같은 조회 조건·응답 형식의 캐시를 찾아 재사용하는 기준이다.
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String = id

    // 브랜치 목록·커밋 목록·파일 트리 구분. 내용의 구조와 캐시 유효기간을 결정한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    val type: GithubCacheType = type

    // 캐시를 얻은 GitHub 조회 주소와 쿼리. 같은 조회를 재검증하고 서로 다른 조회 조건을 구분하는 데 쓴다.
    @Column(name = "request_url", nullable = false, columnDefinition = "TEXT")
    val requestUrl: String = requestUrl

    // GitHub 조회 결과를 보관해 외부 호출 없이 응답하는 데 쓴다.
    // 커밋 캐시는 페이지별 응답 객체의 JSON 배열이며, 배열 인덱스로 페이지 존재 여부를 확인한다.
    @Column(name = "content", nullable = false, columnDefinition = "JSON")
    var content: String = content
        private set

    // GitHub가 준 응답 검증자 원문. If-None-Match로 변경 여부를 확인하며, 없으면 일반 GET을 사용한다.
    @Column(name = "etag", columnDefinition = "TEXT")
    var etag: String? = etag
        private set

    // 캐시를 받거나 GitHub에서 유효성을 마지막으로 확인한 시각. 변경 없음(304)에도 갱신한다.
    // 캐시 만료 판단과 최대 보관 개수 초과 시 오래된 캐시를 교체하는 기준으로 쓴다. 캐시 조회만으로는 갱신하지 않는다.
    @Column(name = "cached_at", nullable = false)
    var cachedAt: Instant = cachedAt
        private set

    // JPA 낙관적 잠금용 수정 버전. 동시 갱신이 캐시 내용·시각을 덮어쓰는 충돌을 저장 시 감지한다.
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        private set

    init {
        require(id.isNotBlank() && id.length <= 100) { "캐시 식별자가 올바르지 않습니다." }
        require(requestUrl.isNotBlank()) { "조회 주소가 필요합니다." }
        validateContent(content)
        validateEtag(etag)
    }

    fun isExpired(at: Instant): Boolean =
        when (type) {
            GithubCacheType.BRANCHES -> !at.isBefore(cachedAt.plus(Duration.ofMinutes(10)))
            GithubCacheType.COMMITS -> !at.isBefore(cachedAt.plus(Duration.ofMinutes(1)))
            GithubCacheType.FILE_TREE -> false
        }

    fun replaceContent(
        content: String,
        at: Instant,
        etag: String? = null,
    ) {
        validateContent(content)
        validateEtag(etag)
        require(!at.isBefore(cachedAt)) { "재검증 시각을 과거로 변경할 수 없습니다." }
        this.content = content
        this.etag = etag
        cachedAt = at
    }

    fun confirmUnchanged(at: Instant) {
        require(!at.isBefore(cachedAt)) { "재검증 시각을 과거로 변경할 수 없습니다." }
        cachedAt = at
    }

    // 추가 커밋 페이지나 하위 트리 조회 결과를 기존 내용에 합친 전체 값을 받는다.
    // 기존 캐시와 같은 이력·트리임을 외부에서 확인한 뒤 호출한다. 추가 조회로 유효성을 확인했으므로 시각도 갱신한다.
    // 다른 이력이면 기존 내용을 합치지 말고 replaceContent로 새로 받은 내용만 저장한다.
    fun supplementContent(
        content: String,
        at: Instant,
    ) {
        require(type == GithubCacheType.COMMITS || type == GithubCacheType.FILE_TREE) {
            "커밋 또는 파일 트리 캐시만 내용을 보완할 수 있습니다."
        }
        validateContent(content)
        require(!at.isBefore(cachedAt)) { "캐싱 시각을 과거로 변경할 수 없습니다." }
        this.content = content
        cachedAt = at
    }

    private fun validateContent(content: String) {
        require(content.isNotBlank()) { "캐시 내용이 필요합니다." }
    }

    private fun validateEtag(etag: String?) {
        require(etag == null || etag.isNotBlank()) { "ETag 값이 올바르지 않습니다." }
    }

    companion object {
        const val MAX_ENTRIES: Int = 1000
    }
}
