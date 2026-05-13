package com.fastcampus.fcboard.domain

import com.fastcampus.fcboard.exception.PostNotUpdatableException
import com.fastcampus.fcboard.service.dto.PostUpdateRequestDto
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany

@Entity
class Post(
  title: String,
  content: String,
  createdBy: String,
  tags: List<String> = emptyList<String>(),
) : BaseEntity(createdBy) {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0L

  var title: String = title
    protected set
  var content: String = content
    protected set

  @OneToMany(mappedBy = "post", orphanRemoval = true, cascade = [CascadeType.ALL])
  var comments: MutableList<Comment> = mutableListOf()
    protected set

  @OneToMany(mappedBy = "post", orphanRemoval = true, cascade = [CascadeType.ALL])
  var tags: MutableList<Tag> = tags.map { Tag(it, this, createdBy) }.toMutableList()
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
