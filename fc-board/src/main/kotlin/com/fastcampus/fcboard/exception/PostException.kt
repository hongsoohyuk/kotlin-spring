package com.fastcampus.fcboard.exception

open class PostException(
  message: String,
) : RuntimeException(message)

class PostNotFoundException : PostException("Post not found")

class PostNotUpdatableException : PostException("Post not updatable")

class PostNotDeletableException : PostException("Post not deletable")
