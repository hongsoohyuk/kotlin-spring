package com.fastcampus.fcboard.service

import com.fastcampus.fcboard.exception.CommentNotDeletableException
import com.fastcampus.fcboard.exception.CommentNotFoundException
import com.fastcampus.fcboard.exception.PostNotFoundException
import com.fastcampus.fcboard.repository.CommentRepository
import com.fastcampus.fcboard.repository.PostRepository
import com.fastcampus.fcboard.service.dto.CommentCreateRequestDto
import com.fastcampus.fcboard.service.dto.CommentUpdateRequestDto
import com.fastcampus.fcboard.service.dto.toEntity
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentService(
  private val postRepository: PostRepository,
  private val commentRepository: CommentRepository,
) {
  @Transactional
  fun createComment(
    postId: Long,
    commentCreateRequestDto: CommentCreateRequestDto,
  ): Long {
    val post = postRepository.findByIdOrNull(postId) ?: throw PostNotFoundException()
    return commentRepository.save(commentCreateRequestDto.toEntity(post)).id
  }

  @Transactional
  fun updateComment(
    commentId: Long,
    commentUpdateRequestDto: CommentUpdateRequestDto,
  ): Long {
    val comment = commentRepository.findByIdOrNull(commentId) ?: throw CommentNotFoundException()
    comment.update(commentUpdateRequestDto)
    return comment.id
  }

  @Transactional
  fun deleteComment(
    commentId: Long,
    deletedBy: String,
  ): Long {
    val comment = commentRepository.findByIdOrNull(commentId) ?: throw CommentNotFoundException()
    if (comment.createdBy != deletedBy) {
      throw CommentNotDeletableException()
    }
    commentRepository.delete(comment)
    return commentId
  }
}
