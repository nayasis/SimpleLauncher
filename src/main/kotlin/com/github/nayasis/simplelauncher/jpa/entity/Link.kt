package com.github.nayasis.simplelauncher.jpa.entity

import com.github.nayasis.simplelauncher.jpa.entity.base.BaseEntity
import kotlinx.serialization.Serializable
import org.hibernate.annotations.Type
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Lob

@Entity
class Link: BaseEntity() {

    @Id @GeneratedValue
    var id: Long = 0

    @Column
    var title: String? = null

    var group: String? = null

    var path: String? = null

    val relativePath: String? = null

    @Column @Type(type = "yes_no")
    var showConsole: Boolean = false

    var option: String? = null

    var optionPrefix: String? = null

    var commandPrev: String? = null
    var commandNext: String? = null

    @Column(name="desc") @Lob
    var description: String? = null

    @Column @Lob
    var keyword: Set<String>? = null

    @Column @Lob
    var icon: ByteArray? = null

    var executeCount: Int = 0
    var lastExecDate: LocalDateTime? = null

}