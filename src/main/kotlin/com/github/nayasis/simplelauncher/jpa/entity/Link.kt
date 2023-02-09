package com.github.nayasis.simplelauncher.jpa.entity

import com.github.nayasis.kotlin.basica.core.extention.ifEmpty
import com.github.nayasis.kotlin.basica.core.io.Paths
import com.github.nayasis.kotlin.basica.core.io.div
import com.github.nayasis.kotlin.basica.core.io.exists
import com.github.nayasis.kotlin.basica.core.io.invariantPath
import com.github.nayasis.kotlin.basica.core.io.pathString
import com.github.nayasis.kotlin.basica.core.io.toRelativeOrSelf
import com.github.nayasis.kotlin.basica.core.string.invariantSeparators
import com.github.nayasis.kotlin.basica.core.string.toFile
import com.github.nayasis.kotlin.basica.core.string.toPath
import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.basica.etc.error
import com.github.nayasis.kotlin.javafx.misc.toBinary
import com.github.nayasis.kotlin.javafx.misc.toIconImage
import com.github.nayasis.kotlin.javafx.misc.toImage
import com.github.nayasis.simplelauncher.common.Context
import com.github.nayasis.simplelauncher.common.ICON_NEW
import com.github.nayasis.simplelauncher.common.toKeyword
import com.github.nayasis.simplelauncher.jpa.converter.StringSetConverter
import javafx.scene.image.Image
import mslinks.ShellLink
import mu.KotlinLogging
import org.hibernate.annotations.DynamicUpdate
import java.io.File
import java.io.Serializable
import java.nio.file.Path
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Lob

private val logger = KotlinLogging.logger {}

const val ICON_IMAGE_TYPE = "png"

@Entity
@DynamicUpdate
class Link: Cloneable, Serializable {

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

    @Column @Lob @Convert(converter = StringSetConverter::class)
    var wordsAll: Set<String>? = null

    @Column @Lob
    var wordsKeyword: Set<String>? = null

    @Column @Lob
    var wordsGroup: Set<String>? = null

    @Column @Lob
    var icon: ByteArray? = null

    @Column
    var executeCount: Int = 0

    @Column
    var lastExecDate: LocalDateTime? = null

    constructor()
    constructor(file: Path): this(file.toFile())
    constructor(file: File) {
        title = file.nameWithoutExtension
        setPath(file)
        setIcon(file)
        if(file.isDirectory()) {
            if( Platforms.isWindows) {
                commandPrefix = "cmd /c explorer"
            }
        } else {
            when(file.extension) {
                "lnk" -> resolveMicrosoftLnk(file)
                "jar" -> commandPrefix = "java -jar"
                "xls", "xlsx" -> {
                    if(Platforms.isWindows) {
                       commandPrefix = "cmd /c start excel"
                    }
                }
            }
        }
    }

    fun setPath(file: File) {
        path = file.invariantSeparatorsPath
        relativePath = file.toPath().toRelativeOrSelf(Paths.applicationRoot).invariantPath
    }

    private fun resolveMicrosoftLnk(file: File) {

        if( ! Platforms.isWindows ) return

        val link = ShellLink(file)

        relativePath = link.relativePath.invariantSeparators()
        path = link.linkInfo.localBasePath.invariantSeparators()
        argument = link.cmdArgs
        description = link.name

        try {
            link.iconLocation.ifEmpty { path }.let { setIcon(it!!.toFile()) }
        } catch (e: Throwable) {
            logger.error(e)
        }

    }

    fun setIcon(file: File): Image? {
        return file.toIconImage().firstOrNull()?.let {
            setIcon(it)
            it
        }
    }

    fun setIcon(image: Image) {
        icon = image.toBinary(ICON_IMAGE_TYPE)
    }

    fun getIconImage(): Image {
        return if (icon == null || icon!!.isEmpty()) ICON_NEW.toImage()!! else try {
            icon.toImage()!!
        } catch (e: Exception) {
            ICON_NEW.toImage()!!
        }
    }

    fun toPath(): Path? {
        runCatching {
            var p = path!!.toPath()
                if( p.exists() ) return p
            p = Paths.applicationRoot / path.ifEmpty{""}
                if( p.exists() ) return p
            p = Paths.applicationRoot / relativePath.ifEmpty{""}
                if( p.exists() ) {
                    path = p.pathString
                    Context.linkService.save(this)
                    return p
                }
        }
        return null
    }

    public override fun clone(): Link {
        return super.clone() as Link
    }

    fun generateKeyword(): Link {
        wordsAll     = listOfNotNull(group,title,description).joinToString(" ").toKeyword()
        wordsKeyword = listOfNotNull(title,description).joinToString(" ").toKeyword()
        wordsGroup   = group?.toKeyword()
        return this
    }

}