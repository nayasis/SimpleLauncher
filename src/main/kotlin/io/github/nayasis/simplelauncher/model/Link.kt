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
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.ScriptExecuteQuery
import kotlin.io.path.div

private val logger = KotlinLogging.logger {}

const val ICON_IMAGE_TYPE = "png"

@KomapperEntity
@KomapperTable(name = "TB_LINK_TEST")
data class Link(
    @KomapperId
    @KomapperAutoIncrement
    var id: Long = 0,
    @KomapperColumn(name = "title")
    var title: String? = null,
    @KomapperColumn(name = "a_group")
    var group: String? = null,
    @KomapperColumn(name = "path")
    var path: String? = null,
    @KomapperColumn(name = "relative_path")
    var relativePath: String? = null,
    @KomapperColumn(name = "show_console")
    var showConsole: Boolean = false,
    @KomapperColumn(name = "execute_each")
    var executeEach: Boolean = true,
    @KomapperColumn(name = "argument")
    var argument: String? = null,
    @KomapperColumn(name = "icon")
    var icon: ByteArray? = null,
    @KomapperColumn(name = "command_prefix")
    var commandPrefix: String? = null,
    @KomapperColumn(name = "command_prev")
    var commandPrev: String? = null,
    @KomapperColumn(name = "command_next")
    var commandNext: String? = null,
    @KomapperColumn(name = "description")
    var description: String? = null,
    @KomapperColumn(name = "hashtag")
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

//    init {
//        indexing()
//    }

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
        return id != other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        fun createTable(): ScriptExecuteQuery {
            return QueryDsl.executeScript("""
                CREATE TABLE IF NOT EXISTS TB_LINK_TEST (
                    -- id             BIGINT PRIMARY KEY AUTO_INCREMENT,
                    id             BIGINT generated always as identity not null,
                    title          VARCHAR(300),
                    a_group        VARCHAR(255),
                    path           VARCHAR(2000),
                    relative_path  VARCHAR(2000),
                    show_console   BOOLEAN     DEFAULT FALSE,
                    execute_each   BOOLEAN     DEFAULT TRUE,
                    argument       VARCHAR(2000),
                    icon           BLOB,
                    command_prefix VARCHAR(2000),
                    command_prev   VARCHAR(2000),
                    command_next   VARCHAR(2000),
                    description    TEXT,
                    hashtag        VARCHAR(2000),
                    exe_count      INT         DEFAULT 0,
                    executed_at    DATETIME,
                    created_at     DATETIME not null,
                    updated_at     DATETIME not null,
                    constraint pk_TB_LINK_TEST primary key(id)
                );                
            """.trimIndent())

            /**
             create table if not exists TB_LINK_TEST (
               id bigint generated always as identity not null,
               title varchar(500),
               a_group varchar(500),
               path varchar(500),
               relative_path varchar(500),
               show_console bool not null,
               execute_each bool not null,
               argument varchar(500),
               icon binary,
               command_prefix varchar(500),
               command_prev varchar(500),
               command_next varchar(500),
               description varchar(500),
               hashtag varchar(500),
               exe_count integer not null,
               executed_at timestamp,
               created_at timestamp not null,
               updated_at timestamp not null,
               constraint pk_TB_LINK_TEST primary key(id))
             */

        }
    }

}

//object Links: Table("TB_LINK_TEST") {
//    val id            = long("id").autoIncrement()
//    val title         = varchar("title", 300).nullable()
//    val group         = varchar("a_group", 255).nullable()
//    val path          = varchar("path", 2000).nullable()
//    val relativePath  = varchar("relative_path", 2000).nullable()
//    val showConsole   = bool("show_console").default(false)
//    val executeEach   = bool("execute_each").default(true)
//    val argument      = varchar("argument", 2000).nullable()
//    val commandPrefix = varchar("command_prefix", 2000).nullable()
//    val commandPrev   = varchar("command_prev", 2000).nullable()
//    val commandNext   = varchar("command_next", 2000).nullable()
//    val description   = text("desc").nullable()
//    val hashtag       = varchar("hashtag", 2000).nullable()
//    val icon          = blob("icon").nullable()
//    val executeCount  = integer("exe_count").default(0)
//    val executedAt    = datetime("executed_at").nullable()
//    val createdAt     = datetime("created_at").default(LocalDateTime.now())
//    val updatedAt     = datetime("updated_at").default(LocalDateTime.now())
//    override val primaryKey = PrimaryKey(arrayOf(id, title),"pk_$tableName")
//}