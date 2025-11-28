package io.github.nayasis.simplelauncher.model

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.LargeText
import com.dshatz.exposed_crud.Varchar
import io.github.nayasis.kotlin.basica.core.extension.ifEmpty
import io.github.nayasis.kotlin.basica.core.extension.runIfNotEmpty
import io.github.nayasis.kotlin.basica.core.io.Paths
import io.github.nayasis.kotlin.basica.core.io.exists
import io.github.nayasis.kotlin.basica.core.io.invariantPath
import io.github.nayasis.kotlin.basica.core.io.toRelativeOrSelf
import io.github.nayasis.kotlin.basica.core.string.runIfNotBlank
import io.github.nayasis.kotlin.basica.core.string.toPath
import io.github.nayasis.kotlin.basica.etc.Platforms
import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.javafx.misc.resize
import io.github.nayasis.kotlin.javafx.misc.toBinary
import io.github.nayasis.kotlin.javafx.misc.toIconImage
import io.github.nayasis.kotlin.javafx.misc.toImage
import io.github.nayasis.simplelauncher.common.Context
import io.github.nayasis.simplelauncher.common.toKeyword
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.scene.image.Image
import mslinks.ShellLink
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime
import javax.imageio.ImageIO
import kotlin.io.path.div

private val logger = KotlinLogging.logger {}

const val ICON_IMAGE_TYPE = "png"


@Entity(name = "TB_LINK_TEST")
data class Link(

    @Id(autoGenerate = true)
    var id: Long = -1,
    @Varchar(length = 300)
    var title: String? = null,
    @Column(name = "a_group")
    @Varchar(length = 300)
    var group: String? = null,
    @Varchar(length = 2000)
    var path: String? = null,
    @Varchar(length = 2000)
    var relativePath: String? = null,
    var showConsole: Boolean = false,
    var executeEach: Boolean = true,
    @Varchar(length = 2000)
    var argument: String? = null,
    var icon: ByteArray? = null,
    @Varchar(length = 2000)
    var commandPrefix: String? = null,
    @Varchar(length = 2000)
    var commandPrev: String? = null,
    @Varchar(length = 2000)
    var commandNext: String? = null,
    @LargeText
    var description: String? = null,
    @Varchar(length = 2000)
    var hashtag: String? = null,
    @Column(name = "exe_count")
    var executeCount: Int = 0,
    var executedAt: LocalDateTime? = null,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {

    @Transient
    val keywordTitle: HashSet<String> = HashSet()
    @Transient
    val keywordGroup: HashSet<String> = HashSet()
    @Transient
    var iconImage: Image? = null
        get() {
            if( field == null && icon != null ) {
                field = runCatching { icon?.toImage() }.getOrNull()
            }
            return field
        }
        set(value) {
            field = value
            icon = value?.toBinary(ICON_IMAGE_TYPE)
        }

    fun setPath(file: File) {
        this.path = file.invariantSeparatorsPath
        relativePath = file.toPath().toRelativeOrSelf(Paths.applicationRoot).invariantPath
    }

    constructor(file: File): this(
        title = file.nameWithoutExtension,
    ) {
        setIcon(file)
        setPath(file)
        when {
            file.isDirectory() && Platforms.isWindows -> {
                commandPrefix = "cmd /c explorer"
            }
            else -> {
                when(file.extension) {
                    "lnk" -> {
                        if(Platforms.isWindows) {
                            bindLink(file)
                        }
                    }
                    "jar" -> {
                        commandPrefix = "java -jar"
                    }
                    "xls", "xlsx" -> {
                        if(Platforms.isWindows) {
                            commandPrefix = "cmd /c start excel"
                        }
                    }
                }
            }
        }
    }

    private fun bindLink(file: File) {
        if( ! Platforms.isWindows ) return
        val link = ShellLink(file)
        path = link.linkInfo.localBasePath
        relativePath = link.relativePath
        argument = link.cmdArgs
        description = link.name
        runCatching {
            link.iconLocation.ifEmpty { path }?.toPath()?.let { setIcon(it.toFile()) }
        }.onFailure { e -> logger.error(e) }
    }

    fun setIcon(file: File): Image? {
        return when(file.extension.lowercase()) {
            "jpg", "jpeg", "png" -> file.toImage()
            "gif" -> runCatching { ImageIO.read(file).toBinary(ICON_IMAGE_TYPE).toImage() }.getOrNull()
            "ico" -> runCatching { file.toIconImage().firstOrNull() }.getOrNull()
            else  -> runCatching { file.toIconImage().firstOrNull() }.getOrNull()
        }?.let { img ->
            runCatching { img.resize(128) }.getOrElse { img }
        }.also { image ->
            try {
                icon = image?.toBinary(ICON_IMAGE_TYPE)
            } catch (e: Exception) {
                logger.error(e) { ">> file : $file" }
                throw e
            }
        }
    }

    fun toPath(): Path? {
        // 1. convert path directly
        path?.let { runCatching { it.toPath() }.getOrNull() }
            ?.takeIf { it.exists() }
            ?.let { return it }

        // 2. combine applicationRoot with path
        path?.takeIf { it.isNotEmpty() }
            ?.let { Paths.applicationRoot / it }
            ?.takeIf { it.exists() }
            ?.let { return it }
        
        // 3. use relativePath
        relativePath?.takeIf { it.isNotEmpty() }
            ?.let { Paths.applicationRoot / it }
            ?.takeIf { it.exists() }
            ?.let { p ->
                path = p.invariantPath
                Context.linkService.save(this, false)
                return p
            }

        return null
    }

    fun refreshIndex(): Link {
        keywordTitle.run {
            clear()
            title.runIfNotBlank { addAll(it.toKeyword()) }
            hashtag.runIfNotEmpty { addAll(it.toKeyword()) }
        }
        keywordGroup.run {
            group.runIfNotBlank { addAll(it.toKeyword()) }
        }
        return this
    }

    fun clone(): Link {
        return this.let { Link(
            title         = it.title,
            group         = it.group,
            path          = it.path,
            relativePath  = it.relativePath,
            showConsole   = it.showConsole,
            executeEach   = it.executeEach,
            argument      = it.argument,
            icon          = it.icon?.clone(),
            commandPrefix = it.commandPrefix,
            commandPrev   = it.commandPrev,
            commandNext   = it.commandNext,
            description   = it.description,
            hashtag       = it.hashtag,
            executeCount  = it.executeCount,
            executedAt    = it.executedAt,
            createdAt     = it.createdAt,
            updatedAt     = it.updatedAt,
        )}
    }

    override fun equals(other: Any?): Boolean {
        return (other is Link)
                && this.id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Link(id=$id, title=$title, group=$group, path=$path, relativePath=$relativePath, showConsole=$showConsole, executeEach=$executeEach, argument=$argument, commandPrefix=$commandPrefix, commandPrev=$commandPrev, commandNext=$commandNext, description=$description, hashtag=$hashtag, executeCount=$executeCount, executedAt=$executedAt, createdAt=$createdAt, updatedAt=$updatedAt)"
    }

}