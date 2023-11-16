package com.github.nayasis.simplelauncher.model

import au.com.console.kassava.kotlinToString
import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.nayasis.kotlin.basica.core.extension.ifEmpty
import com.github.nayasis.kotlin.basica.core.extension.ifNotEmpty
import com.github.nayasis.kotlin.basica.core.io.Paths
import com.github.nayasis.kotlin.basica.core.io.exists
import com.github.nayasis.kotlin.basica.core.io.invariantPath
import com.github.nayasis.kotlin.basica.core.io.toRelativeOrSelf
import com.github.nayasis.kotlin.basica.core.string.ifNotBlank
import com.github.nayasis.kotlin.basica.core.string.toPath
import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.basica.etc.error
import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.misc.copy
import com.github.nayasis.kotlin.javafx.misc.toBinary
import com.github.nayasis.kotlin.javafx.misc.toIconImage
import com.github.nayasis.kotlin.javafx.misc.toImage
import com.github.nayasis.simplelauncher.common.Context
import com.github.nayasis.simplelauncher.common.toKeyword
import javafx.scene.image.Image
import mslinks.ShellLink
import mu.KotlinLogging
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.update
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.io.path.div

private val logger = KotlinLogging.logger {}

const val ICON_IMAGE_TYPE = "png"

object Links: Table("TB_LINK_TEST") {
    val id            = long("id").autoIncrement()
    val title         = varchar("title", 300).nullable()
    val group         = varchar("a_group", 255).nullable()
    val path          = varchar("path", 2000).nullable()
    val relativePath  = varchar("relative_path", 2000).nullable()
    val showConsole   = bool("show_console").default(false)
    val executeEach   = bool("execute_each").default(true)
    val argument      = varchar("argument", 2000).nullable()
    val commandPrefix = varchar("command_prefix", 2000).nullable()
    val commandPrev   = varchar("command_prev", 2000).nullable()
    val commandNext   = varchar("command_next", 2000).nullable()
    val description   = text("desc").nullable()
    val hashtag       = varchar("hashtag", 2000).nullable()
    val icon          = blob("icon").nullable()
    val executeCount  = integer("exe_count").default(0)
    val executedAt    = datetime("executed_at").nullable()
    val createdAt     = datetime("created_at").default(LocalDateTime.now())
    val updatedAt     = datetime("updated_at").default(LocalDateTime.now())
    override val primaryKey = PrimaryKey(arrayOf(id, title),"pk_$tableName")
}

data class Link(
    var id: Long = 0,
    var title: String? = null,
    var group: String? = null,
    var path: String? = null,
    var relativePath: String? = null,
    var showConsole: Boolean = false,
    var executeEach: Boolean = true,
    var argument: String? = null,
    var icon: Image? = null,
    var commandPrefix: String? = null,
    var commandPrev: String? = null,
    var commandNext: String? = null,
    var description: String? = null,
    var hashtag: String? = null,
    var executeCount: Int = 0,
    var executedAt: LocalDateTime? = null,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {

    val isNew: Boolean
        get() = id <= 0

    val keywordTitle: HashSet<String> = HashSet()
    val keywordGroup: HashSet<String> = HashSet()

    init {
        indexing()
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

    private fun bindLink(link: File) {
        if( ! Platforms.isWindows ) return
        val link = ShellLink(link)
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
        }.getOrNull().also { icon = it }
    }

    @JsonIgnore
    fun toPath(): Path? {

        var p = path?.toPath() ?: return null
        if(p.exists()) return p

        p = Paths.applicationRoot / path.ifEmpty { "" }
        if(p.exists()) return p

        p = Paths.applicationRoot / relativePath.ifEmpty { "" }
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
            title.ifNotBlank { addAll(it.toKeyword()) }
            hashtag.ifNotEmpty { addAll(it.toKeyword()) }
        }
        keywordGroup.run {
            group.ifNotBlank { addAll(it.toKeyword()) }
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
            icon          = it.icon?.copy(),
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

    override fun toString(): String {
        return kotlinToString(arrayOf(
            Link::id,
            Link::title,
            Link::group,
            Link::path,
            Link::relativePath,
            Link::showConsole,
            Link::executeEach,
            Link::argument,
            Link::commandPrefix,
            Link::commandPrev,
            Link::commandNext,
            Link::description,
            Link::hashtag,
            Link::executeCount,
            Link::executedAt,
            Link::createdAt,
            Link::updatedAt,
        ))
    }
}

fun UpdateBuilder<*>.from(entity: Link) {
    if(entity.id > 0) this[Links.id] = entity.id
    this[Links.title]         = entity.title
    this[Links.group]         = entity.group
    this[Links.path]          = entity.path
    this[Links.relativePath]  = entity.relativePath
    this[Links.showConsole]   = entity.showConsole
    this[Links.executeEach]   = entity.executeEach
    this[Links.argument]      = entity.argument
    this[Links.commandPrefix] = entity.commandPrefix
    this[Links.commandPrev]   = entity.commandPrev
    this[Links.commandNext]   = entity.commandNext
    this[Links.hashtag]       = entity.hashtag?.let { Reflector.toJson(it) }
    this[Links.icon]          = entity.icon?.toBinary(ICON_IMAGE_TYPE)?.let { ExposedBlob(it) }
    this[Links.executeCount]  = entity.executeCount
    this[Links.executedAt]    = entity.executedAt
    this[Links.createdAt]     = entity.createdAt
    this[Links.updatedAt]     = entity.updatedAt
}

fun ResultRow.toLink(): Link {
    return this.let { row ->Link(
        id            = row[Links.id],
        title         = row[Links.title],
        group         = row[Links.group],
        path          = row[Links.path],
        relativePath  = row[Links.relativePath],
        showConsole   = row[Links.showConsole],
        executeEach   = row[Links.executeEach],
        argument      = row[Links.argument],
        commandPrefix = row[Links.commandPrefix],
        commandPrev   = row[Links.commandPrev],
        commandNext   = row[Links.commandNext],
        description   = row[Links.description],
        hashtag       = row[Links.hashtag]?.let { Reflector.toObject(it) },
        icon          = row[Links.icon]?.bytes?.toImage(),
        executeCount  = row[Links.executeCount],
        executedAt    = row[Links.executedAt],
        createdAt     = row[Links.createdAt],
        updatedAt     = row[Links.updatedAt],
    )}
}

fun Links.save(link: Link) {
    if(link.id <= 0) {
        insert { it.from(link) }.let { row ->
            link.id = row[id]
        }
    } else {
        update({
            Links.id eq link.id
        }) {
            it.from(link)
        }
    }
}