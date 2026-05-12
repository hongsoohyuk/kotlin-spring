package com.fastcampus.fcboard.service.dto

import com.fastcampus.fcboard.domain.Comment

data class CommentResponseDto(
  val id: Long,
  val content: String,
  val createdAt: String,
  val createdBy: String,
//  val updatedAt: String,
//  val updatedBy: String,
)

fun Comment.toResponseDto(): CommentResponseDto =
  CommentResponseDto(
    id,
    content,
    createdAt.toString(),
    createdBy,
  )
