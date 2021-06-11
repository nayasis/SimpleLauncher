package com.github.nayasis.simplelauncher.jpa.entity

import com.github.nayasis.simplelauncher.jpa.entity.base.BaseEntity
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob

@Entity
class Config: BaseEntity {

    @Id
    var key: String = ""

    @Column @Lob
    var value: String? = null

    constructor()

    constructor(key: String, value: String? = null) {
        this.key = key
        this.value = value
    }

}