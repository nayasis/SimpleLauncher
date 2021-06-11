package com.github.nayasis.simplelauncher.jpa.repository

import com.github.nayasis.simplelauncher.jpa.entity.Link
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LinkRepository: JpaRepository<Link,Long> {

    fun findAllByOOrderByGroupAndTitle()

}
