package com.github.nayasis.simplelauncher

import com.github.nayasis.kotlin.basica.etc.error
import com.github.nayasis.kotlin.basica.exception.rootCause
import com.github.nayasis.kotlin.basica.model.Messages
import com.github.nayasis.kotlin.basica.net.Networks
import com.github.nayasis.kotlin.javafx.preloader.NPreloader
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.kotlin.javafx.stage.Stages
import com.github.nayasis.simplelauncher.common.Environment
import com.github.nayasis.simplelauncher.common.LoggerConfig
import com.github.nayasis.simplelauncher.common.SimpleDiContainer
import com.github.nayasis.simplelauncher.model.Links
import com.github.nayasis.simplelauncher.service.LinkExecutor
import com.github.nayasis.simplelauncher.service.LinkService
import com.github.nayasis.simplelauncher.view.Main
import com.github.nayasis.simplelauncher.view.Splash
import javafx.application.Platform
import mu.KotlinLogging
import org.h2.Driver
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.App
import tornadofx.FX
import tornadofx.launch
import tornadofx.runLater
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {

    FX.dicontainer = SimpleDiContainer().apply {
        set(LinkService())
        set(LinkExecutor())
        set(Environment(args))
    }

    Networks.ignoreCerts()
    Messages.loadFromResource("/message/**.prop")
    Stages.defaultIcons.add("/image/icon/favicon.png")
    LoggerConfig(Environment((args))).initialize()
    setPreloader(Splash::class)
    setupDefaultExceptionHandler()
    connectDb()
    try {
        launch<Simplelauncher>(args)
    } catch (e: Exception) {
        logger.error(e)
    }
}

class Simplelauncher: App(Main::class) {

}

fun setPreloader(preloader: KClass<out NPreloader>) {
    System.setProperty("javafx.preloader", preloader.jvmName)
    System.setProperty("java.awt.headless", "false")
}

fun setupDefaultExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        if (Platform.isFxApplicationThread()) {
            runCatching {
                runLater {
                    Dialog.error(e.rootCause)
                }
            }.onFailure { logger.error(it) }
        } else {
            logger.error(e)
        }
    }
}

private fun connectDb() {
    Database.connect(
        url      = "jdbc:h2:file:./db/simplelauncher",
        driver   = Driver::class.qualifiedName!!,
//        user     = "user",
//        password = "1234",
    )
    transaction {
        SchemaUtils.create(Links)
    }

}