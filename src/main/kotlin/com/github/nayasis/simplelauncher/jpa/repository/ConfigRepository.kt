package com.github.nayasis.simplelauncher.jpa.repository

import com.github.nayasis.simplelauncher.jpa.entity.Config
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConfigRepository: JpaRepository<Config,String> {
}