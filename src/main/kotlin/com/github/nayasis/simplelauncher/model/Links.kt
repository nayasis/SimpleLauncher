package com.github.nayasis.simplelauncher.model

import au.com.console.kassava.kotlinToString
import com.github.nayasis.kotlin.basica.core.extension.ifEmpty
import com.github.nayasis.kotlin.basica.core.extension.ifNotEmpty
import com.github.nayasis.kotlin.basica.core.io.Paths
import com.github.nayasis.kotlin.basica.core.io.div
import com.github.nayasis.kotlin.basica.core.io.exists
import com.github.nayasis.kotlin.basica.core.io.invariantPath
import com.github.nayasis.kotlin.basica.core.io.pathString
import com.github.nayasis.kotlin.basica.core.io.toRelativeOrSelf
import com.github.nayasis.kotlin.basica.core.string.ifNotBlank
import com.github.nayasis.kotlin.basica.core.string.invariantSeparators
import com.github.nayasis.kotlin.basica.core.string.toFile
import com.github.nayasis.kotlin.basica.core.string.toPath
import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.basica.etc.error
import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.misc.toBinary
import com.github.nayasis.kotlin.javafx.misc.toIconImage
import com.github.nayasis.kotlin.javafx.misc.toImage
import com.github.nayasis.simplelauncher.common.Context
import com.github.nayasis.simplelauncher.common.ICON_NEW
import com.github.nayasis.simplelauncher.common.toKeyword
import javafx.scene.image.Image
import mslinks.ShellLink
import mu.KotlinLogging
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import java.io.File
import java.nio.file.Path
import java.sql.Clob
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

const val ICON_IMAGE_TYPE = "png"

object Links: LongIdTable("TB_LINK_TEST") {
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
    val hashtag       = jsonb<LinkedHashSet<String>>("hashtag", 2000, { Reflector.toObject(it) }).default(LinkedHashSet())
    val icon          = blob("icon").nullable()
    val executeCount  = integer("exe_count").default(0)
    val executedAt    = datetime("executed_at").default(LocalDateTime.now())
    val createdAt     = datetime("created_at").default(LocalDateTime.now())
    val updatedAt     = datetime("updated_at").default(LocalDateTime.now())
}

class Link(id: EntityID<Long>): LongEntity(id) {

    companion object: LongEntityClass<Link>(Links)

    var title          by Links.title
    var group          by Links.group
    var path           by Links.path
    var relativePath   by Links.relativePath
    var showConsole    by Links.showConsole
    var executeEach    by Links.executeEach
    var argument       by Links.argument
    var commandPrefix  by Links.commandPrefix
    var commandPrev    by Links.commandPrev
    var commandNext    by Links.commandNext
    var description    by Links.description
    var hashtag        by Links.hashtag
    var executeCount   by Links.executeCount
    var executedAt     by Links.executedAt
    var createdAt      by Links.createdAt
    var updatedAt      by Links.updatedAt

    @field:Transient
    var keywordTitle  = HashSet<String>()
    @field:Transient
    var keywordGroup  = HashSet<String>()

    private var _icon by Links.icon

    var icon: Image?
        get() = _icon?.bytes?.ifNotEmpty { runCatching { it.toImage() }.getOrNull() } ?: ICON_NEW.toImage()
        set(value) {
            _icon = value?.let { ExposedBlob(it.toBinary(ICON_IMAGE_TYPE)) }
        }

    fun setPath(file: File) {
        path = file.invariantSeparatorsPath
        relativePath = file.toPath().toRelativeOrSelf(Paths.applicationRoot).invariantPath
    }

    private fun setFromMicrosoftLink(link: File) {
        if( ! Platforms.isWindows ) return
        val link = ShellLink(link)

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
        return runCatching { file.toIconImage().firstOrNull() }.getOrNull().also { icon = it }
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
                val self = this
                TODO("update to db ?")
                Context.linkService.save(this)
                return p
            }
        }
        return null
    }

    fun generateKeyword() {
        listOf(keywordTitle,keywordGroup).forEach { it.clear() }
        title.ifNotBlank { keywordTitle.addAll(it.toKeyword()) }
        group.ifNotBlank { keywordGroup.addAll(it.toKeyword()) }
        keywordTitle.addAll(hashtag)
    }

    fun of(file: File): Link {
        title = file.nameWithoutExtension
        setPath(file)
        setIcon(file)
        if(file.isDirectory()) {
            if( Platforms.isWindows) {
                commandPrefix = "cmd /c explorer"
            }
        } else {
            when(file.extension) {
                "lnk" -> setFromMicrosoftLink(file)
                "jar" -> commandPrefix = "java -jar"
                "xls", "xlsx" -> {
                    if(Platforms.isWindows) {
                        commandPrefix = "cmd /c start excel"
                    }
                }
            }
        }
        return this
    }

    fun clone(): Link {
        val self = this
        return Link.new {
            title         = self.title
            group         = self.group
            path          = self.path
            relativePath  = self.relativePath
            showConsole   = self.showConsole
            executeEach   = self.executeEach
            argument      = self.argument
            commandPrefix = self.commandPrefix
            commandPrev   = self.commandPrev
            commandNext   = self.commandNext
            description   = self.description
            hashtag       = self.hashtag.let { LinkedHashSet(it) }
            executeCount  = self.executeCount
            executedAt    = self.executedAt
            createdAt     = self.createdAt
            updatedAt     = self.updatedAt
            _icon         = self._icon?.bytes?.clone()?.let { ExposedBlob(it) }
            keywordTitle.apply { clear() }.apply { addAll(self.keywordTitle) }
            keywordGroup.apply { clear() }.apply { addAll(self.keywordGroup) }
        }
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

fun <T> Table.jsonb(name: String, length: Int, fromJson: (String) -> T) = Links.registerColumn<T>(name,JsonbColumnType(length, fromJson))

class JsonbColumnType<T>(length: Int, private val fromJson: (String) -> T) : VarCharColumnType(colLength = length) {
    override fun notNullValueToDB(value: Any): Any {
        return Reflector.toJson(value)
    }
    override fun valueFromDB(value: Any): Any {
        val text = when (value) {
            is Clob -> value.characterStream.readText()
            is ByteArray -> String(value)
            else -> value.toString()
        }
        return fromJson.invoke(text) as Any
    }
}

val Entity<*>.isNew: Boolean
    get() = this.id._value == null