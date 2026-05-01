package com.fastcampus.fcboard.controller

import com.fastcampus.fcboard.controller.dto.PostCreateRequest
import com.fastcampus.fcboard.controller.dto.PostDetailResponse
import com.fastcampus.fcboard.controller.dto.PostSearchRequest
import com.fastcampus.fcboard.controller.dto.PostSummaryResponse
import com.fastcampus.fcboard.controller.dto.PostUpdateRequest
import com.fastcampus.fcboard.controller.dto.toDto
import com.fastcampus.fcboard.service.PostService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class PostController(
  private val postService: PostService, // DI??
) {
  @PostMapping("/posts")
  fun createPost(
    @RequestBody postCreateRequest: PostCreateRequest,
  ): Long = postService.createPost(postCreateRequest.toDto())

  @PutMapping("/posts/{id}")
  fun updatePost(
    @PathVariable id: Long,
    @RequestBody postUpdateRequest: PostUpdateRequest,
  ): Long = postService.updatePost(id, postUpdateRequest.toDto())

  @DeleteMapping("/posts/{id}")
  fun deletePost(
    @PathVariable id: Long,
    @RequestParam createdBy: String,
  ): Long = postService.deletePost(id, createdBy)

  @GetMapping("/posts/{id}")
  fun getPost(
    @PathVariable id: Long,
  ): PostDetailResponse = PostDetailResponse(1L, "", "", LocalDateTime.now().toString(), "")

  @GetMapping("/posts")
  fun getPosts(
    pageable: Pageable,
    postSearchRequest: PostSearchRequest,
  ): Page<PostSummaryResponse> {
    println(postSearchRequest)
    return Page.empty()
  }
}
