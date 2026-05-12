package com.fastcampus.fcboard.service

import com.fastcampus.fcboard.domain.Comment
import com.fastcampus.fcboard.domain.Post
import com.fastcampus.fcboard.exception.CommentNotUpdatableException
import com.fastcampus.fcboard.exception.PostNotFoundException
import com.fastcampus.fcboard.repository.CommentRepository
import com.fastcampus.fcboard.repository.PostRepository
import com.fastcampus.fcboard.service.dto.CommentCreateRequestDto
import com.fastcampus.fcboard.service.dto.CommentUpdateRequestDto
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull

@SpringBootTest
class CommentServiceTest(
  @Autowired private val commentService: CommentService,
  @Autowired private val commentRepository: CommentRepository,
  @Autowired private val postRepository: PostRepository,
  service: CommentService,
) : BehaviorSpec({
  given("create comment") {
    postRepository.save(Post("title", "content", "person"))
    When(
      "valid input"
    ) {
      val commentId = commentService.createComment(1L, CommentCreateRequestDto("comment content", "comment creator"))
      then("check, a comment is created") {
        commentId shouldBeGreaterThan 0L
        val comment = commentRepository.findByIdOrNull(commentId)
        comment shouldNotBe null
        comment?.content shouldBe "comment content"
        comment?.createdBy shouldBe "comment creator"
      }
    }

    When(
      "no post"
    ) {
      then("error, post not found") {
        shouldThrow<PostNotFoundException> {
          commentService.createComment(9999L, CommentCreateRequestDto("comment content", "comment creator"))
        }
      }
    }
  }
  given("update comment") {
    val post = postRepository.save(Post("post title", "post content", "post person"))
    val saved = commentRepository.save(Comment("comment content", post, "comment person"))
    When(
      "valid input"
    ) {
      val updatedId =
        commentService.updateComment(
          saved.id,
          CommentUpdateRequestDto(
            "updated comment content",
            "comment person"
          )
        )
      then("check, a comment is updated") {
        updatedId shouldBe saved.id
        val updated = commentRepository.findByIdOrNull(saved.id)
        updated shouldNotBe null
        updated?.content shouldBe "updated comment content"
        updated?.createdBy shouldBe "comment person"
      }
    }
    When(
      "creator != updater"
    ) {
      then("error, update not allowed") {
        shouldThrow<CommentNotUpdatableException> {
          commentService.updateComment(saved.id, CommentUpdateRequestDto("comment content", "comment creator"))
        }
      }
    }
  }

  given("delete comment") {
    val post = postRepository.save(Post("post title", "post content", "post person"))
    val saved1 = commentRepository.save(Comment("comment content1", post, "comment person1"))
    val saved2 = commentRepository.save(Comment("comment content2", post, "comment person2"))
    When(
      "valid input"
    ) {
      val updatedId =
        commentService.deleteComment(
          saved1.id,
          "comment person1"
        )
      then("check, a comment is updated") {
      }
    }
    When(
      "creator != updater"
    ) {
      then("error, update not allowed") {
        shouldThrow<CommentNotUpdatableException> {
          commentService.deleteComment(saved2.id, "comment person1")
        }
      }
    }
  }
})
