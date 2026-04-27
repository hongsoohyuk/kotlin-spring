package com.fastcampus.fcboard.controller.dto

data class PostDetailResponse(
  val id: Long,
  val title: String,
  val content: String,
  val createdAt: String,
  val createdBy: String
)
