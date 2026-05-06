package com.fastcampus.fcboard.controller.dto

import com.fastcampus.fcboard.service.dto.PostDetailResponseDto
import org.springframework.data.jpa.domain.AbstractPersistable_.id

data class PostDetailResponse(
  val id: Long,
  val title: String,
  val content: String,
  val createdAt: String,
  val createdBy: String,
)

fun PostDetailResponseDto.toResponse() = PostDetailResponse(
  id = id,
  title = title,
  content = content,
  createdBy = createdBy,
  createdAt = createdAt,
)
