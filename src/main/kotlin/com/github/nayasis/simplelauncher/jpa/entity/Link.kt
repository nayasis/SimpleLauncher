package com.github.nayasis.simplelauncher.jpa.entity

import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.Type
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Lob

@Entity
@DynamicUpdate
class Link {

    @Id @GeneratedValue
    var id: Long = 0

    @Column
    var title: String? = null

    @Column(name = "group_a")
    var group: String? = null

    @Column
    var path: String? = null

    @Column
    val relativePath: String? = null

    @Column @Type(type = "yes_no")
    var showConsole: Boolean = false

    @Column
    var option: String? = null

    @Column
    var optionPrefix: String? = null

    @Column
    var commandPrev: String? = null

    @Column
    var commandNext: String? = null

    @Column(name="desc") @Lob
    var description: String? = null

    @Column @Lob
    var keyword: Set<String>? = null

    @Column @Lob
    var icon: ByteArray? = null

    @Column
    var executeCount: Int = 0

    @Column
    var lastExecDate: LocalDateTime? = null

}