package com.fastcampus.fcboard.controller.dto

data class CommentResponse(
  val id: Long,
  val content: String,
  val createdAt: String,
  val createdBy: String,
)
