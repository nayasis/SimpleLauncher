package com.github.nayasis.simplelauncher.service

import com.github.nayasis.simplelauncher.jpa.entity.Config
import com.github.nayasis.simplelauncher.jpa.repository.ConfigRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ConfigService(
    private val repository: ConfigRepository
) {

    operator fun get(key: String): String? {
        return repository.findByIdOrNull(key)?.value
    }

    @Transactional
    operator fun set(key: String, value: String?) {
        if( repository.existsById(key) ) {
            repository.findById(key).get().value = value
        } else {
            repository.save(Config(key,value))
        }
    }

}