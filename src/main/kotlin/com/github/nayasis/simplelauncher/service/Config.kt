package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.io.exists
import com.github.nayasis.kotlin.basica.core.io.readText
import com.github.nayasis.kotlin.basica.core.io.writeText
import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.property.SizeProperty
import com.github.nayasis.kotlin.javafx.property.StageProperty
import com.github.nayasis.simplelauncher.view.HistorySet
import com.github.nayasis.terminalfx.kt.config.TerminalSize
import java.nio.file.Paths
import kotlin.reflect.jvm.jvmName

private val configPath = Paths.get("conf").resolve("${Config::class.jvmName}.json")

class Config {

    var filePickerInitialDirectory: String? = null
    var stageMain: StageProperty? = null
    var stageTerminal: StageProperty? = null
    var stageHelp: SizeProperty? = null
    var terminalSize: TerminalSize? = null
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