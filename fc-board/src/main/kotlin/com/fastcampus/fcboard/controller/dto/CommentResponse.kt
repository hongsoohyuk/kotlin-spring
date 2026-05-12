package com.fastcampus.fcboard.controller.dto

import com.fastcampus.fcboard.service.dto.CommentResponseDto

data class CommentResponse(
  val id: Long,
  val content: String,
  val createdAt: String,
  val createdBy: String,
)

fun CommentResponseDto.toResponse() =
  CommentResponse(
    id = id,
    content = content,
    createdAt = createdAt,
    createdBy = createdBy,
  )