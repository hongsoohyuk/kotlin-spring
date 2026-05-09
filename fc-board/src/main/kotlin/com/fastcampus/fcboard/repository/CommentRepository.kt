package com.fastcampus.fcboard.repository

import com.fastcampus.fcboard.domain.Comment
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository :
  JpaRepository<Comment, Long>,
  CustomPostRepository

// interface CustomRepository {
//  fun findPageBy(
//    pageRequest: Pageable,
//    postSearchRequestDto: PostSearchRequestDto,
//  ): Page<Post>
// }

// class CustomPostRepositoryImpl :
//  QuerydslRepositorySupport(Post::class.java),
//  CustomPostRepository {
//  override fun findPageBy(
//    pageRequest: Pageable,
//    postSearchRequestDto: PostSearchRequestDto,
//  ): Page<Post> {
//    val result =
//      from(post)
//        .where(
//          postSearchRequestDto.title?.let { post.title.contains(it) },
//          postSearchRequestDto.createdBy?.let { post.title.contains(it) },
//        ).orderBy(post.createdAt.desc())
//        .offset(pageRequest.offset)
//        .limit(pageRequest.pageSize.toLong())
//        .fetchResults()
//    return PageImpl(result.results, pageRequest, result.total)
//  }
// }
