package com.fastcampus.fcboard.exception

open class CommentException(
  message: String,
) : RuntimeException(message)

class CommentNotFoundException : CommentException("Comment not found")

class CommentNotUpdatableException : CommentException("Comment not updatable")

class CommentNotDeletableException : CommentException("Comment not deletable")
