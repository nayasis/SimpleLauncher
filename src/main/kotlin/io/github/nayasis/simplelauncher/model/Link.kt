package io.github.nayasis.simplelauncher.model

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.LargeText
import io.github.nayasis.kotlin.basica.core.extension.ifEmpty
import io.github.nayasis.kotlin.basica.core.io.Paths
import io.github.nayasis.kotlin.basica.core.io.exists
import io.github.nayasis.kotlin.basica.core.io.invariantPath
import io.github.nayasis.kotlin.basica.core.io.toRelativeOrSelf
import io.github.nayasis.kotlin.basica.core.string.toPath
import io.github.nayasis.kotlin.basica.etc.Platforms
import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.swing.misc.resize
import io.github.nayasis.kotlin.swing.misc.toBinary
import io.github.nayasis.kotlin.swing.misc.toBufferedImage
import io.github.nayasis.kotlin.swing.misc.toIconImages
import io.github.nayasis.kotlin.swing.misc.toSystemIconImage
import io.github.nayasis.simplelauncher.common.Context
import io.github.oshai.kotlinlogging.KotlinLogging
import mslinks.ShellLink
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime
import javax.imageio.ImageIO
import kotlin.io.path.div

private val logger = KotlinLogging.logger {}

const val ICON_IMAGE_TYPE = "png"
private const val ICON_MAX_SIZE = 128

@Entity(name = "TB_LINK_TEST")
data class Link(

    @Id(autoGenerate = true)
    @Column
    var id: Long = -1,
    @Column(length = 300)
    var title: String? = null,
    var group: HashSet<String> = hashSetOf(),
    @Column(name = "a_group", length = 300)
    var groupJson: String? = null,
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
    @Column
    var icon: ByteArray? = null,
    @Column(length = 2000)
    var commandPrefix: String? = null,
    @Column(length = 2000)
    var commandPrev: String? = null,
    @Column(length = 2000)
    var commandNext: String? = null,
    @Column
    @LargeText
    var description: String? = null,
    var hashtag: HashSet<String> = hashSetOf(),
    @Column(name = "hashtag", length = 2000)
    var hashtagJson: String? = null,
    @Column(name = "exe_count")
    var executeCount: Int = 0,
    @Column
    var executedAt: LocalDateTime? = null,
    @Column
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {

    // decoded once from [icon]; reset whenever the icon bytes are replaced through this class
    private var decodedIcon: BufferedImage? = null
    private var decodedFrom: ByteArray? = null

    var iconImage: BufferedImage?
        get() {
            val bytes = icon ?: return null
            if( decodedFrom !== bytes ) {
                decodedIcon = runCatching { bytes.toBufferedImage() }.getOrNull()
                decodedFrom = bytes
            }
            return decodedIcon
        }
        set(value) {
            icon = value?.toBinary(ICON_IMAGE_TYPE)
            decodedIcon = value
            decodedFrom = icon
        }

    init {
        if (group.isEmpty() && !groupJson.isNullOrBlank()) {
            group = decodeStoredTokens(groupJson)
        }
        if (hashtag.isEmpty() && !hashtagJson.isNullOrBlank()) {
            hashtag = decodeStoredHashtags(hashtagJson)
        }
        syncTokenStorage()
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

    /**
     * use the icon of [file]: the image itself, or the icon the file carries.
     *
     * @return the icon, or null when none could be read (the current icon is kept then)
     */
    fun setIcon(file: File): BufferedImage? {
        return readIcon(file)?.also { iconImage = it }
    }

    fun toPath(): Path? {
        // 1. convert path directly
        path?.let { runCatching { it.toPath() }.getOrNull() }
            ?.takeIf { it.exists() }
            ?.let { return it }

        // 2. combine applicationRoot with path
        path?.takeIf { it.isNotEmpty() }
            ?.let { runCatching { Paths.applicationRoot.resolve(it) }.getOrNull() }
            ?.takeIf { it.exists() }
            ?.let { return it }

        // 3. use relativePath
        relativePath?.takeIf { it.isNotEmpty() }
            ?.let { Paths.applicationRoot / it }
            ?.takeIf { it.exists() }
            ?.let { p ->
                path = p.invariantPath
                Context.linkService.launchSave(this)
                return p
            }

        return null
    }

    fun syncTokenStorage(): Link {
        group = normalizeTokens(group)
        groupJson = encodeTokens(group)
        hashtag = normalizeHashtags(hashtag)
        hashtagJson = encodeHashtags(hashtag)
        return this
    }

    fun syncHashtagStorage(): Link = syncTokenStorage()

    fun clone(): Link {
        return this.let { Link(
            title         = it.title,
            group         = HashSet(it.group),
            groupJson     = it.groupJson,
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
            hashtag       = HashSet(it.hashtag),
            hashtagJson   = it.hashtagJson,
            executeCount  = it.executeCount,
            executedAt    = it.executedAt,
            createdAt     = it.createdAt,
            updatedAt     = it.updatedAt,
        )}
    }

    override fun equals(other: Any?): Boolean {
        return other is Link && this.id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Link(id=$id, title=$title, group=$group, path=$path, relativePath=$relativePath, showConsole=$showConsole, executeEach=$executeEach, argument=$argument, commandPrefix=$commandPrefix, commandPrev=$commandPrev, commandNext=$commandNext, description=$description, hashtag=$hashtag, executeCount=$executeCount, executedAt=$executedAt, createdAt=$createdAt, updatedAt=$updatedAt)"
    }

    companion object {

        /**
         * read the icon of [file] without touching any link: the image itself for image files,
         * the embedded icon for icon files, otherwise the icon the operating system shows for it.
         */
        fun readIcon(file: File): BufferedImage? {
            val image = runCatching {
                when(file.extension.lowercase()) {
                    "jpg", "jpeg", "png", "gif", "bmp" -> ImageIO.read(file)
                    "ico" -> file.toIconImages().maxByOrNull { it.width }
                    else  -> file.toSystemIconImage(ICON_MAX_SIZE)
                }
            }.onFailure { logger.error(it) { ">> file : $file" } }.getOrNull() ?: return null
            return runCatching { image.resize(ICON_MAX_SIZE) }.getOrElse { image }
        }

    }

}
