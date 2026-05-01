package com.fastcampus.fcboard.controller

import com.fastcampus.fcboard.domain.Post
import com.fastcampus.fcboard.exception.PostNotDeletableException
import com.fastcampus.fcboard.exception.PostNotFoundException
import com.fastcampus.fcboard.exception.PostNotUpdatableException
import com.fastcampus.fcboard.repository.PostRepository
import com.fastcampus.fcboard.service.PostService
import com.fastcampus.fcboard.service.dto.PostCreateRequestDto
import com.fastcampus.fcboard.service.dto.PostUpdateRequestDto
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import jakarta.persistence.EntityNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull

@SpringBootTest
class PostServiceTest(
  @Autowired private val postService: PostService,
  private val postRepository: PostRepository,
  service: PostService,
) : BehaviorSpec({
    given("ㄱㅔ시글 작성시") {
      When("게시글 입력이 정상적") {
        val postId =
          postService.createPost(
            PostCreateRequestDto(
              title = "title",
              content = "content",
              createdBy = "name",
            ),
          )
        then("게시글이 정상적으로 생성됨.") {
          postId shouldBeGreaterThan 0L
          val post = postRepository.findByIdOrNull(postId)
          post shouldNotBe null
          post?.title shouldBe "title"
          post?.content shouldBe "content"
          post?.createdBy shouldBe "name"
        }
      }
    }

    given("게시글 수정시") {
      val saved = postRepository.save(Post(title = "title", content = "content", createdBy = "name"))
      When("정상 수정시") {
        val updatedId =
          postService.updatePost(
            saved.id,
            PostUpdateRequestDto(
              title = "updated title",
              content = "updated name",
              updatedBy = "name",
            ),
          )

        then("게시글이 정상적으로 수정됨을 확인.") {
          saved.id shouldBe updatedId
          val updated = postRepository.findByIdOrNull(updatedId)
          updated?.title shouldBe "updated title"
          updated?.content shouldBe "updated name"
          updated?.updatedBy shouldBe "name"
        }
      }

      When("When post not found") {
        then("error, Post not found.") {
          shouldThrow<PostNotFoundException> {
            postService.updatePost(
              999L,
              PostUpdateRequestDto(
                title = "updated title",
                content = "updated name",
                updatedBy = "updated name",
              ),
            )
          }
        }
      }

      When("Not same creator") {
        then("error, Post not same creator") {
          shouldThrow<PostNotUpdatableException> {
            postService.updatePost(
              1L,
              PostUpdateRequestDto(
                title = "updated title",
                content = "updated name",
                updatedBy = "updated name",
              ),
            )
          }
        }
      }
    }
    given("given delete post") {
      val saved = postRepository.save(Post(title = "title", content = "content", createdBy = "name"))

      When("valid deletion") {
        val postId = postService.deletePost(saved.id, "name")
        then("check post is deleted") {
          postId shouldBe saved.id
          postRepository.findByIdOrNull(saved.id) shouldBe null
        }
      }
      When("not same creator") {
        val saved = postRepository.save(Post(title = "title", content = "content", createdBy = "name"))

        then("error, Post not same creator") {
          shouldThrow<PostNotDeletableException> { postService.deletePost(saved.id, "unknown") }
        }
      }
    }
  })
