package com.github.nayasis.simplelauncher

import com.github.nayasis.kotlin.basica.core.extension.ifNotEmpty
import com.github.nayasis.kotlin.basica.model.Messages
import com.github.nayasis.kotlin.basica.net.Networks
import com.github.nayasis.kotlin.javafx.app.FxApp
import com.github.nayasis.kotlin.javafx.preloader.BasePreloader
import com.github.nayasis.kotlin.javafx.stage.Stages
import com.github.nayasis.simplelauncher.common.Context
import com.github.nayasis.simplelauncher.model.Links
import com.github.nayasis.simplelauncher.service.LinkExecutor
import com.github.nayasis.simplelauncher.service.LinkService
import com.github.nayasis.simplelauncher.view.Main
import com.github.nayasis.simplelauncher.view.Splash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import mu.KotlinLogging
import org.apache.commons.cli.CommandLine
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.launch
import java.util.*
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    Networks.ignoreCerts()
    Messages.loadFromResource("/message/**.prop")
    BasePreloader.set(Splash::class)
    launch<Simplelauncher>(args)
}

class Simplelauncher: FxApp(Main::class), CoroutineScope  {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.JavaFx

    override fun onStart(command: CommandLine) {
        Stages.defaultIcons.add("/image/icon/favicon.png")
        environment.get<String>("simplelauncher.locale").ifNotEmpty { locale ->
            Locale.setDefault(Locale.forLanguageTag(locale))
        }
        logger.debug { ">> initialized" }
        connectDb()
        logger.debug { ">> db connected" }
        ctx.apply {
            set(LinkService())
            set(LinkExecutor())
        }
        logger.debug { ">> bean initialized" }
    }

    private fun connectDb() {
        Database.connect(
            url      = environment["simplelauncher.datasource.url"] ?: "",
            driver   = environment["simplelauncher.datasource.driver-class"] ?: "",
            user     = environment["simplelauncher.datasource.user"] ?: "",
            password = environment["simplelauncher.datasource.password"] ?: "",
        )
        transaction {
            SchemaUtils.create(Links)
        }
    }
}

