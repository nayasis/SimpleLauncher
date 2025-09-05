package io.github.nayasis.simplelauncher.model

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.nayasis.kotlin.basica.core.extension.ifEmpty
import io.github.nayasis.kotlin.basica.core.extension.isEmpty
import io.github.nayasis.kotlin.basica.core.extension.runIfNotEmpty
import io.github.nayasis.kotlin.basica.core.io.Paths
import io.github.nayasis.kotlin.basica.core.io.exists
import io.github.nayasis.kotlin.basica.core.io.invariantPath
import io.github.nayasis.kotlin.basica.core.io.toRelativeOrSelf
import io.github.nayasis.kotlin.basica.core.string.runIfNotBlank
import io.github.nayasis.kotlin.basica.core.string.toPath
import io.github.nayasis.kotlin.basica.etc.Platforms
import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.javafx.misc.toBinary
import io.github.nayasis.kotlin.javafx.misc.toIconImage
import io.github.nayasis.kotlin.javafx.misc.toImage
import io.github.nayasis.simplelauncher.common.Context
import io.github.nayasis.simplelauncher.common.toKeyword
import io.github.nayasis.simplelauncher.model.types.BlobByteArray
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.scene.image.Image
import mslinks.ShellLink
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime
import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperColumn
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.core.type.BlobByteArray
import org.komapper.core.type.ClobString
import kotlin.io.path.div

private val logger = KotlinLogging.logger {}

const val ICON_IMAGE_TYPE = "png"

@KomapperEntity
@KomapperTable(name = "TB_LINK_TEST")
data class Link(
    @KomapperId
    @KomapperAutoIncrement
    var id: Long = 0,
    @KomapperColumn(name = "title", length = 300)
    var title: String? = null,
    @KomapperColumn(name = "a_group", length = 300)
    var group: String? = null,
    @KomapperColumn(name = "path", length = 2000)
    var path: String? = null,
    @KomapperColumn(name = "relative_path", length = 2000)
    var relativePath: String? = null,
    @KomapperColumn(name = "show_console")
    var showConsole: Boolean = false,
    @KomapperColumn(name = "execute_each")
    var executeEach: Boolean = true,
    @KomapperColumn(name = "argument", length = 2000)
    var argument: String? = null,
    @KomapperColumn(name = "icon", alternateType = BlobByteArray::class)
    var icon: ByteArray? = null,
    @KomapperColumn(name = "command_prefix", length = 2000)
    var commandPrefix: String? = null,
    @KomapperColumn(name = "command_prev", length = 2000)
    var commandPrev: String? = null,
    @KomapperColumn(name = "command_next", length = 2000)
    var commandNext: String? = null,
    @KomapperColumn(name = "description", alternateType = ClobString::class)
    var description: String? = null,
    @KomapperColumn(name = "hashtag", length = 2000)
    var hashtag: String? = null,
    @KomapperColumn(name = "exe_count")
    var executeCount: Int = 0,
    @KomapperColumn(name = "executed_at")
    var executedAt: LocalDateTime? = null,
    @KomapperColumn(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @KomapperColumn(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {

    val isNew: Boolean
        get() = id <= 0

    val keywordTitle: HashSet<String> = HashSet()
    val keywordGroup: HashSet<String> = HashSet()

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

    @JsonIgnore
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

    @JsonIgnore
    fun setIcon(file: File): Image? {
        return runCatching {
            file.toIconImage().firstOrNull()
        }.getOrNull().also { image -> 
            icon = image?.toBinary(ICON_IMAGE_TYPE)
        }
    }

    @JsonIgnore
    fun toPath(): Path? {
        var p = path?.let { runCatching { it.toPath() }.getOrNull() } ?: return null
            if(p.exists()) return p
        p = Paths.applicationRoot / path.ifEmpty { "" }
            if(p.exists()) return p
            if(relativePath.isEmpty()) return null
        p = Paths.applicationRoot / relativePath!!
            if(p.exists()) {
                path = p.invariantPath
                Context.linkService.save(this, false)
                return p
            }
        return null
    }

    fun indexing(): Link {
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
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Link
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}