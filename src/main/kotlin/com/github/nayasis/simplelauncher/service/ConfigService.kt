package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.path.notExists
import com.github.nayasis.kotlin.basica.core.path.readText
import com.github.nayasis.kotlin.basica.core.path.writeText
import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.property.StageProperty
import org.springframework.stereotype.Component
import java.nio.file.Paths
import kotlin.reflect.jvm.jvmName

class ConfigService { companion object {

    private val configPath = Paths.get("conf").resolve("${this::class.jvmName}.properties")

    var filePickerInitialDirectory: String? = null
        get() = this[::filePickerInitialDirectory.name]
        set(value) {
            field = value.also { this[::filePickerInitialDirectory.name] = it }
        }

    var stageMain: StageProperty? = null
        get() = this[::stageMain.name]?.let { Reflector.toObject(it) }
        set(value) {
            field = value.also { this[::stageMain.name] = Reflector.toJson(it) }
        }

    var stageTerminal: StageProperty? = null
        get() = this[::stageTerminal.name]?.let { Reflector.toObject(it) }
        set(value) {
            field = value.also { this[::stageTerminal.name] = Reflector.toJson(it) }
        }

    private val config = loadConfig()

    private fun loadConfig(): MutableMap<String,String?> {
        return if( configPath.notExists() )
            HashMap()
        else {
            try {
                configPath.readText().let { Reflector.toObject(it) }
            } catch (e: Exception) {
                HashMap()
            }
        }
    }

    fun clear() {
        config.clear()
    }

    operator fun get(key: String): String? = config[key]

    operator fun set(key: String, value: String?) {
        config[key] = value
    }

    fun save() = Reflector.toJson(config).let { configPath.writeText(it) }

}}