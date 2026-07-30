package com.fastcampus.fcboard.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import com.fastcampus.fcboard.util.RedisUtil

@RestController
class RedisController(
  private val redisUtil: RedisUtil,
) {
  @GetMapping("/redis")
  fun getRedisCount(): Long {
    redisUtil.increment("test")
    return redisUtil.getCount("test") ?: 0L
  }
}