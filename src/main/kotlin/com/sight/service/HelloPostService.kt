package com.sight.service

import com.sight.repository.HelloPostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class HelloPost(
    val id: Long,
    val category: String?,
    val title: String,
    val date: Instant,
)

@Service
class HelloPostService(
    private val helloPostRepository: HelloPostRepository,
) {
    @Transactional(readOnly = true)
    fun listHelloPosts(limit: Int): List<HelloPost> {
        return helloPostRepository.findRecentPosts(limit).map { post ->
            HelloPost(
                id = post.id,
                category = post.category,
                title = post.title,
                date = post.date,
            )
        }
    }
}
