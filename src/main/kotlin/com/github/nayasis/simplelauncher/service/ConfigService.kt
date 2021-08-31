package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.path.notExists
import com.github.nayasis.kotlin.basica.core.path.readText
import com.github.nayasis.kotlin.basica.core.path.writeText
import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.property.StageProperty
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.common.Context
import com.github.nayasis.simplelauncher.jpa.entity.Config
import javafx.beans.property.SimpleObjectProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tornadofx.onChange
import java.nio.file.Paths
import kotlin.reflect.jvm.jvmName

private val configPath = Paths.get("conf").resolve("${Dialog::class.jvmName}.properties")

@Component
class ConfigService {

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

    fun commit() = Reflector.toJson(config).let { configPath.writeText(it) }


}