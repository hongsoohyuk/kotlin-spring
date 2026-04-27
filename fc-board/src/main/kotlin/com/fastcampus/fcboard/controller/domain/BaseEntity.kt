package com.fastcampus.fcboard.controller.domain

import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import java.time.LocalDateTime

@MappedSuperclass
abstract class BaseEntity (
  createdBy: String
){
  val createdBy: String = createdBy
  val createdAt: LocalDateTime = LocalDateTime.now()
  var updatedBy: String? = null
    protected set
  var updatedAt: LocalDateTime? = null
    protected set

  fun update(updatedBy: String) {
    this.updatedBy= updatedBy
    this.updatedAt = LocalDateTime.now()
  }
}
