package com.fastcampus.fcboard.controller.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.springframework.data.annotation.CreatedBy

@Entity
class Post (
  createdBy: String, title:String, content:String
): BaseEntity(createdBy){
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0

  var title: String = title
    protected set
  var content: String = content
    protected set
}
