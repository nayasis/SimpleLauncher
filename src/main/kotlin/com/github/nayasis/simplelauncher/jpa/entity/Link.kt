package com.github.nayasis.simplelauncher.jpa.entity

import com.github.nayasis.kotlin.basica.core.path.FOLDER_SEPARATOR
import com.github.nayasis.kotlin.basica.core.path.pathString
import com.github.nayasis.kotlin.basica.core.path.rootPath
import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.javafx.misc.Images
import com.github.nayasis.simplelauncher.common.ICON_NEW
import javafx.scene.image.Image
import mslinks.ShellLink
import org.hibernate.annotations.DynamicUpdate
import java.io.File
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Lob

const val ICON_IMAGE_TYPE = "png"

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

    constructor()
    constructor(file: File) {
        title = file.nameWithoutExtension
        setPath(file)
        setIcon(file)
        if(file.isDirectory) {
            if( Platforms.isWindows) {
                commandPrefix = "cmd /c explorer"
            }
        } else {
            when(file.extension) {
                "lnk" -> resolveMicrosoftLnk(file)
                "jar" -> commandPrefix = "java -jar"
            }
        }
    }

    fun setPath(file: File) {
        path = file.toString()
        relativePath = file.toPath().relativize(rootPath()).pathString
    }

    private fun resolveMicrosoftLnk(file: File) {

        if( ! Platforms.isWindows ) return

        val link = ShellLink(file)

        relativePath = link.relativePath
        path = "${link.workingDir}${FOLDER_SEPARATOR}${link.relativePath}"
        argument = link.cmdArgs
        description = link.name

        link.iconLocation.ifEmpty { path }.let { setIcon(File(it)) }

    }

    fun setIcon(file: File): Image? {
        return Images.toIconImage(file).firstOrNull()?.let {
            setIcon(it)
            it
        }
    }

    fun setIcon(image: Image) {
        icon = Images.toBinary(image, ICON_IMAGE_TYPE)
    }

    fun getIconImage(): Image {
        if( icon == null || icon!!.isEmpty() )
            return Images.toImage(ICON_NEW)!!
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