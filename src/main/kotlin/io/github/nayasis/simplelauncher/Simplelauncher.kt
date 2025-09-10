package io.github.nayasis.simplelauncher

import io.github.nayasis.kotlin.basica.core.extension.runIfNotEmpty
import io.github.nayasis.kotlin.basica.model.Messages
import io.github.nayasis.kotlin.basica.net.Networks
import io.github.nayasis.kotlin.javafx.app.FxApp
import io.github.nayasis.kotlin.javafx.preloader.BasePreloader
import io.github.nayasis.kotlin.javafx.stage.Stages
import io.github.nayasis.simplelauncher.common.KomapperHelper
import io.github.nayasis.simplelauncher.common.KomapperHelper.runQuery
import io.github.nayasis.simplelauncher.model.Link
import io.github.nayasis.simplelauncher.service.LinkExecutor
import io.github.nayasis.simplelauncher.service.LinkService
import io.github.nayasis.simplelauncher.view.Main
import io.github.nayasis.simplelauncher.view.Splash
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import org.apache.commons.cli.CommandLine
import tornadofx.launch
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {

    // turn off external logs
    Logger.getLogger("").level = Level.SEVERE

    Networks.ignoreCerts()
    Messages.loadFromResource("/message/**.prop")
    BasePreloader.set(Splash::class)

    launch<Simplelauncher>(args)
}

class Simplelauncher: FxApp(Main::class), CoroutineScope  {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.JavaFx

    override fun onStart(command: CommandLine) {
        // set favicon
        Stages.defaultIcons.add("/image/icon/favicon.png")

        // set i18n
        environment.get<String>("simplelauncher.locale").runIfNotEmpty { locale ->
            Locale.setDefault(Locale.forLanguageTag(locale))
        }

        KomapperHelper.connectDatabase()

        QueryDsl.create(Meta.link).runQuery()
        logger.debug { ">> database prepared" }

        // initialize beans
        ctx.set(
            LinkService(),
            LinkExecutor(),
        )
        logger.debug { ">> bean initialized" }
    }

}
