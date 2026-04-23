package com.fastcampus.fcboard

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class SampleController {
  @GetMapping("/sample")
  fun sample(): String {
    return "Hello world!"
  }

  @PostMapping("/sample")
  fun samplePost(@RequestParam name: String): String {
    return "Hello $name!"
  }
}
