package com.fastcampus.fcboard.controller

import com.fastcampus.fcboard.util.RedisUtil
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

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
