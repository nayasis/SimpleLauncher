package com.github.nayasis.simplelauncher.jpa.entity.base

import com.github.nayasis.kotlin.basica.reflection.Reflector
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.io.Serializable
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.MappedSuperclass

@MappedSuperclass
class BaseEntity: Serializable {

    @CreationTimestamp
    @Column
    var regDt: LocalDateTime? = null

    @UpdateTimestamp
    @Column
    var updDt: LocalDateTime? = null

    override fun toString(): String {
        return Reflector.toJson(this)
    }

}