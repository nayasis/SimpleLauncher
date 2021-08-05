package com.github.nayasis.simplelauncher.jpa.entity

import com.github.nayasis.kotlin.javafx.misc.Images
import com.github.nayasis.simplelauncher.common.ICON_NEW
import javafx.scene.image.Image
import org.hibernate.annotations.DynamicUpdate
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Lob

@Entity
@DynamicUpdate
class Link: Cloneable {

    @Id @GeneratedValue
    var id: Long = 0

    @Column
    var title: String? = null

    @Column(name="a_group")
    var group: String? = null

    @Column
    var path: String? = null

    @Column
    var relativePath: String? = null

    @Column
    var showConsole: Boolean = false

    @Column
    var eachExecution: Boolean = true

    @Column
    var argument: String? = null

    @Column
    var commandPrefix: String? = null

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

    fun getImageIcon(): Image {
        return try {
            Images.toImage(icon ?: ICON_NEW)!!
        } catch (e: Exception) {
            Images.toImage(ICON_NEW)!!
        }
    }

    public override fun clone(): Link {
        return super.clone() as Link
    }

}