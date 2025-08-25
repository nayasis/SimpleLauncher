package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.core.io.exists
import io.github.nayasis.kotlin.basica.core.io.readText
import io.github.nayasis.kotlin.basica.core.io.writeText
import io.github.nayasis.kotlin.basica.reflection.Reflector
import io.github.nayasis.kotlin.javafx.property.SizeProperty
import io.github.nayasis.kotlin.javafx.property.StageProperty
import io.github.nayasis.simplelauncher.view.HistorySet
import java.nio.file.Paths
import kotlin.reflect.jvm.jvmName

private val configPath = Paths.get("conf").resolve("${Config::class.jvmName}.json")

class Config {

    var filePickerInitialDirectory: String? = null
    var stageMain: StageProperty? = null
    var stageTerminal: StageProperty? = null
    var stageHelp: SizeProperty? = null
//    var terminalSize: TerminalSize? = null
    var historyKeyword = HistorySet<String>(20)
    var lastFocusedRow: Int? = null

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