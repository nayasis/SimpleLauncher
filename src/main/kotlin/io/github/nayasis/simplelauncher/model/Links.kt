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
import io.github.nayasis.simplelauncher.common.Context
import io.github.nayasis.simplelauncher.common.toKeyword
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.scene.image.Image
import mslinks.ShellLink
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Lob
import kotlin.io.path.div

private val logger = KotlinLogging.logger {}

const val ICON_IMAGE_TYPE = "png"

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

@Entity(name = "TB_LINK_TEST")
data class Link(
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long = 0,
    @Column(length = 300)
    var title: String? = null,
    @Column(length = 255, name="a_group")
    var group: String? = null,
    @Column(length = 2000)
    var path: String? = null,
    @Column(length = 2000)
    var relativePath: String? = null,
    @Column
    var showConsole: Boolean = false,
    @Column
    var executeEach: Boolean = true,
    @Column(length = 2000)
    var argument: String? = null,
    @Column @Lob
    var icon: ByteArray? = null,
    @Column(length = 2000)
    var commandPrefix: String? = null,
    @Column(length = 2000)
    var commandPrev: String? = null,
    @Column(length = 2000)
    var commandNext: String? = null,
    @Column @Lob
    var description: String? = null,
    @Column(length = 2000)
    var hashtag: String? = null,
    @Column(name="exe_count")
    var executeCount: Int = 0,
    @Column
    var executedAt: LocalDateTime? = null,
    @Column
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {

    val isNew: Boolean
        get() = id <= 0

    val keywordTitle: HashSet<String> = HashSet()
    val keywordGroup: HashSet<String> = HashSet()

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
        }.getOrNull().also { icon = it?.toBinary(ICON_IMAGE_TYPE) }
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

}

//fun UpdateBuilder<*>.from(entity: Link) {
//    if(entity.id > 0) this[Links.id] = entity.id
//    this[Links.title]         = entity.title
//    this[Links.group]         = entity.group
//    this[Links.path]          = entity.path
//    this[Links.relativePath]  = entity.relativePath
//    this[Links.showConsole]   = entity.showConsole
//    this[Links.executeEach]   = entity.executeEach
//    this[Links.argument]      = entity.argument
//    this[Links.commandPrefix] = entity.commandPrefix
//    this[Links.commandPrev]   = entity.commandPrev
//    this[Links.commandNext]   = entity.commandNext
//    this[Links.description]   = entity.description
//    this[Links.hashtag]       = entity.hashtag
//    this[Links.icon]          = entity.icon?.toBinary(ICON_IMAGE_TYPE)?.let { ExposedBlob(it) }
//    this[Links.executeCount]  = entity.executeCount
//    this[Links.executedAt]    = entity.executedAt
//    this[Links.createdAt]     = entity.createdAt
//    this[Links.updatedAt]     = entity.updatedAt
//}
//
//fun ResultRow.toLink(): Link {
//    return this.let { row -> Link(
//        id            = row[Links.id],
//        title         = row[Links.title],
//        group         = row[Links.group],
//        path          = row[Links.path],
//        relativePath  = row[Links.relativePath],
//        showConsole   = row[Links.showConsole],
//        executeEach   = row[Links.executeEach],
//        argument      = row[Links.argument],
//        commandPrefix = row[Links.commandPrefix],
//        commandPrev   = row[Links.commandPrev],
//        commandNext   = row[Links.commandNext],
//        description   = row[Links.description],
//        hashtag       = row[Links.hashtag],
//        icon          = row[Links.icon]?.bytes?.toImage(),
//        executeCount  = row[Links.executeCount],
//        executedAt    = row[Links.executedAt],
//        createdAt     = row[Links.createdAt],
//        updatedAt     = row[Links.updatedAt],
//    )}
//}
//
//fun Links.save(link: Link) {
//    if(link.id <= 0) {
//        insert { it.from(link) }.let { row ->
//            link.id = row[id]
//        }
//    } else {
//        update({
//            Links.id eq link.id
//        }) {
//            it.from(link)
//        }
//    }
//}