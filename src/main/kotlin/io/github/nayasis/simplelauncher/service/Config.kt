package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.core.io.exists
import io.github.nayasis.kotlin.basica.core.io.readText
import io.github.nayasis.kotlin.basica.core.io.writeText
import io.github.nayasis.kotlin.basica.reflection.Reflector
import io.github.nayasis.kotlin.swing.collection.HistorySet
import io.github.nayasis.kotlin.swing.property.WindowProperty
import io.github.nayasis.simplelauncher.view.SearchFieldState
import java.nio.file.Paths
import kotlin.reflect.jvm.jvmName

private val configPath = Paths.get("conf").resolve("${Config::class.jvmName}.json")

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

    fun save() = configPath.writeText(Reflector.toJson(this,true))

    companion object {
        fun load(): Config {
            if( configPath.exists() ) {
                runCatching {
                    configPath.readText().let {
                        return Reflector.toObject(it)
                    }
                }
            }
            return Config()
        }
    }

}
