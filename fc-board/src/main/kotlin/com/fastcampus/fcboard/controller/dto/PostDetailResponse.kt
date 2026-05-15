package com.fastcampus.fcboard.controller.dto

import com.fastcampus.fcboard.service.dto.PostDetailResponseDto

data class PostDetailResponse(
  val id: Long,
  val title: String,
  val content: String,
  val createdAt: String,
  val createdBy: String,
  val comments: List<CommentResponse> = emptyList<CommentResponse>(),
  val tags: List<String> = emptyList<String>(),
)

fun PostDetailResponseDto.toResponse() =
  PostDetailResponse(
    id = id,
    title = title,
    content = content,
    createdBy = createdBy,
    createdAt = createdAt,
    comments = comments.map { it.toResponse() },
    tags = tags
  )
