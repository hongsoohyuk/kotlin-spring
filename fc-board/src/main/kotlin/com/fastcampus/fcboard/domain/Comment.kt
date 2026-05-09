package com.fastcampus.fcboard.domain

import com.fastcampus.fcboard.exception.PostNotUpdatableException
import com.fastcampus.fcboard.service.dto.PostUpdateRequestDto
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne

@Entity
class Comment(
  content: String,
  post: Post,
  createdBy: String,
) : BaseEntity(createdBy) {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0L

  var content: String = content
    protected set

  @ManyToOne(fetch = FetchType.LAZY)
  var post: Post = post
    protected set

//  fun update(postUpdateRequestDto: PostUpdateRequestDto) {
//    if (this.createdBy != postUpdateRequestDto.updatedBy) {
//      throw PostNotUpdatableException()
//    }
//
//    this.content = postUpdateRequestDto.content
//    super.updatedBy(postUpdateRequestDto.updatedBy)
//  }
}
