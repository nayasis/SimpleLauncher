package com.github.nayasis.simplelauncher

import com.github.nayasis.kotlin.basica.core.extension.ifNotEmpty
import com.github.nayasis.kotlin.basica.model.Messages
import com.github.nayasis.kotlin.basica.net.Networks
import com.github.nayasis.kotlin.javafx.app.FxApp
import com.github.nayasis.kotlin.javafx.preloader.NPreloader
import com.github.nayasis.kotlin.javafx.stage.Stages
import com.github.nayasis.simplelauncher.common.Context
import com.github.nayasis.simplelauncher.common.LoggerConfig
import com.github.nayasis.simplelauncher.model.Links
import com.github.nayasis.simplelauncher.service.LinkExecutor
import com.github.nayasis.simplelauncher.service.LinkService
import com.github.nayasis.simplelauncher.view.Main
import com.github.nayasis.simplelauncher.view.Splash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.javafx.JavaFxDispatcher
import kotlinx.coroutines.javafx.awaitPulse
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.apache.commons.cli.CommandLine
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.launch
import tornadofx.runAsync
import tornadofx.runLater
import java.util.*
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    Networks.ignoreCerts()
    Messages.loadFromResource("/message/**.prop")
    NPreloader.set(Splash::class)
    launch<Simplelauncher>(args)
}

class Simplelauncher: FxApp(Main::class)  {

    override fun onStart(command: CommandLine) {
        logger.debug { ">> start on start" }
        LoggerConfig(environment).initialize()
        Stages.defaultIcons.add("/image/icon/favicon.png")
        environment.get<String>("simplelauncher.locale").ifNotEmpty { locale ->
            Locale.setDefault(Locale.forLanguageTag(locale))
        }
        logger.debug { ">> initialized" }
        NPreloader.notifyProgress(0.1, "initialized")
//        runLater {
//            NPreloader.notifyProgress(0.1, "initialized")
//            logger.debug { ">> preloader: initialized " }
//        }
        connectDb()
        logger.debug { ">> db connected" }
        NPreloader.notifyProgress(0.2, "db connected")
//        runLater {
//            NPreloader.notifyProgress(0.2, "db connected")
//            logger.debug { ">> preloader: db connected" }
//        }
        ctx.apply {
            set(LinkService())
            set(LinkExecutor())
        }
//        runAsync {
//            Context.linkService.loadAll()
//        }
        logger.debug { ">> end on start" }
    }

    override fun onStop() {
        Context.config.save()
    }

    private fun connectDb() {
        Database.connect(
            url      = environment.get("simplelauncher.datasource.url") ?: "",
            driver   = environment.get("simplelauncher.datasource.driver-class") ?: "",
            user     = environment.get("simplelauncher.datasource.user") ?: "",
            password = environment.get("simplelauncher.datasource.password") ?: "",
        )
        transaction {
            SchemaUtils.create(Links)
        }
    }
}

