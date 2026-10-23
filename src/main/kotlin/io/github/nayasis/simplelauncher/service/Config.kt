package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.core.io.exists
import io.github.nayasis.kotlin.basica.core.io.readText
import io.github.nayasis.kotlin.basica.core.io.writeText
import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.basica.reflection.Reflector
import io.github.nayasis.kotlin.swing.collection.HistorySet
import io.github.nayasis.kotlin.swing.property.WindowProperty
import io.github.nayasis.simplelauncher.common.applicationRoot
import io.github.nayasis.simplelauncher.view.SearchFieldState
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.jvm.jvmName

private val logger = KotlinLogging.logger {}

private val configPath = applicationRoot.resolve("conf").resolve("${Config::class.jvmName}.json")

/**
 * persisted application state.
 *
 * unknown properties are ignored when loading, so a renamed or removed property only drops its own value.
 * that is why the window states of the JavaFX version (`stageMain`, `stageTerminal`) are simply not read.
 */
class Config {

    var filePickerInitialDirectory: String? = null
    var windowMain: WindowProperty? = null
    var windowTerminal: WindowProperty? = null
    var historyKeyword = HistorySet<String>(20)
    var historySearch = HistorySet<SearchFieldState>(20)
    var lastSearchKeyword: String? = null
    var lastSearchState: SearchFieldState? = null
    var lastFocusedLinkId: Long? = null
    var descEditorWidth: Double? = null
    var progressDialogX: Double? = null
    var progressDialogY: Double? = null
    var shortcuts = mutableMapOf<String, String>()
    var theme: String? = null
    /** language tag of the chosen message bundle, like "en" or "zh-CN"; null follows the system. */
    var language: String? = null

    fun save() = configPath.writeText(Reflector.toJson(this,true))

    companion object {
        /**
         * the saved state, or the defaults when there is none.
         *
         * a file that cannot be read is moved aside before the defaults are used, so the next save does not
         * overwrite what may still be recovered from it.
         */
        fun load(): Config {
            if( ! configPath.exists() ) return Config()
            return runCatching { Reflector.toObject<Config>(configPath.readText()) }.getOrElse { e ->
                val backup = configPath.resolveSibling("${configPath.fileName}.${LocalDateTime.now().format(BACKUP_SUFFIX)}.broken")
                logger.error(e) { "cannot read $configPath, moved to $backup" }
                runCatching { Files.move(configPath, backup, StandardCopyOption.REPLACE_EXISTING) }.onFailure { logger.error(it) }
                Config()
            }
        }

        private val BACKUP_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    }

}
