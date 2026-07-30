package com.fastcampus.fcboard.service

import com.fastcampus.fcboard.domain.Post
import com.fastcampus.fcboard.exception.PostNotFoundException
import com.fastcampus.fcboard.repository.LikeRepository
import com.fastcampus.fcboard.repository.PostRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.testcontainers.containers.GenericContainer
@SpringBootTest
class LikeServiceTest(
  private val likeService: LikeService,
  private val likeRepository: LikeRepository,
  private val postRepository: PostRepository,
) : BehaviorSpec({
  val redisContainer = GenericContainer<Nothing>("redis:5.0.3-alpine")
  beforeSpec {
    redisContainer.portBindings.add("16379:6379")
    redisContainer.start()
  }

  afterSpec {
    redisContainer.stop()
  }
  given("create like") {
    val saved = postRepository.save(Post("title", "content", "person"))
    When(
      "valid input"
    ) {
      val likeId = likeService.createLike(saved.id, "name")
      then("check, a like is created") {
        val like = likeRepository.findByIdOrNull(likeId)
        like shouldNotBe null
        like?.createdBy shouldBe "name"
      }
    }
    When(
      "no post"
    ) {
      then("error, post not found") {
        shouldThrow<PostNotFoundException> {
          likeService.createLike(9999L, "name")
        }
      }
    }
  }
})
