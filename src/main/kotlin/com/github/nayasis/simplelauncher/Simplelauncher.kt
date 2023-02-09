package com.github.nayasis.simplelauncher

import com.github.nayasis.kotlin.basica.net.Networks
import com.github.nayasis.kotlin.javafx.spring.SpringFxApp
import com.github.nayasis.simplelauncher.common.BootLogger
import com.github.nayasis.simplelauncher.view.Main
import com.github.nayasis.simplelauncher.view.Splash
import mu.KotlinLogging
import org.apache.commons.cli.CommandLine
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContextInitializer
import tornadofx.launch
import java.util.*

private val logger = KotlinLogging.logger {}

@SpringBootApplication
class Simplelauncher: SpringFxApp(Main::class) {

    @Value("\${simplalauncher.locale:}")
    var locale = ""

    private var bootLogger: BootLogger? = BootLogger()

    override fun onStart(command: CommandLine) {
        Locale.setDefault(Locale.forLanguageTag(locale))
        bootLogger?.close()
        bootLogger = null
    }

    override fun setInitializers(): List<ApplicationContextInitializer<*>>? {
        return bootLogger?.getInitializer()?.let { listOf(it) }
    }

}

fun main(args: Array<String>) {
    Networks.trustAllCerts()
    SpringFxApp.run{
        loadMessage("/message/**.prop")
        loadDefaultIcon("/image/icon/favicon.png")
        setPreloader(Splash::class)
    }
    launch<Simplelauncher>(*args)
}