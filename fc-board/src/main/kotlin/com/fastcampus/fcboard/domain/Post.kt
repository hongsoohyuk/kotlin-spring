package com.fastcampus.fcboard.domain

import com.fastcampus.fcboard.exception.PostNotUpdatableException
import com.fastcampus.fcboard.service.dto.PostUpdateRequestDto
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Post(
  title: String,
  content: String,
  createdBy: String,
) : BaseEntity(createdBy) {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0

  var title: String = title
    protected set
  var content: String = content
    protected set

  fun update(postUpdateRequestDto: PostUpdateRequestDto) {
    if (this.createdBy != postUpdateRequestDto.updatedBy) {
      throw PostNotUpdatableException()
    }

    this.title = postUpdateRequestDto.title
    this.content = postUpdateRequestDto.content
    super.updatedBy(postUpdateRequestDto.updatedBy)
  }
}
