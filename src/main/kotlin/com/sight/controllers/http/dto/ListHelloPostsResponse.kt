package com.sight.controllers.http.dto

import com.sight.service.HelloPost
import java.time.Instant

data class ListHelloPostsResponse(
    val posts: List<ListHelloPostResponse>,
) {
    companion object {
        fun from(posts: List<HelloPost>): ListHelloPostsResponse {
            return ListHelloPostsResponse(
                posts = posts.map(ListHelloPostResponse::from),
            )
        }
    }
}

data class ListHelloPostResponse(
    val id: Long,
    val category: String?,
    val title: String,
    val date: Instant,
) {
    companion object {
        fun from(post: HelloPost): ListHelloPostResponse {
            return ListHelloPostResponse(
                id = post.id,
                category = post.category,
                title = post.title,
                date = post.date,
            )
        }
    }
}
