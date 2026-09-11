package io.github.nayasis.simplelauncher

import io.github.nayasis.kotlin.basica.core.extension.runIfNotEmpty
import io.github.nayasis.kotlin.basica.model.Messages
import io.github.nayasis.kotlin.basica.model.Messages.Companion.loadMessages
import io.github.nayasis.kotlin.basica.net.Networks
import io.github.nayasis.kotlin.javafx.app.Environment
import io.github.nayasis.kotlin.javafx.app.FxApp
import io.github.nayasis.kotlin.javafx.app.di.SimpleDiContainer
import io.github.nayasis.kotlin.javafx.preloader.DefaultPreloader
import io.github.nayasis.kotlin.javafx.stage.Stages
import io.github.nayasis.simplelauncher.common.ExposedHelper
import io.github.nayasis.simplelauncher.service.LinkExecutor
import io.github.nayasis.simplelauncher.service.LinkService
import io.github.nayasis.simplelauncher.view.Main
import io.github.nayasis.simplelauncher.view.Splash
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.application.Application
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.apache.commons.cli.CommandLine
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

private val logger = KotlinLogging.logger {}

/**
 * Message bundles, listed one by one on purpose.
 *
 * A glob pattern over the message directory needs a classpath scan, and a GraalVM
 * native image has no classpath to scan, so the glob silently resolves to nothing
 * and every label falls back to its key. Add new bundles here.
 */
private val MESSAGE_BUNDLES = listOf(
    "/message/localization.en.prop",
    "/message/localization.ko.prop",
)

fun main(args: Array<String>) {

    // turn off external logs
    Logger.getLogger("").level = Level.SEVERE

    System.setProperty("javafx.suppressPreviewWarning", "true")
    System.setProperty("javafx.enablePreview", "true")

    Networks.ignoreCerts()
    MESSAGE_BUNDLES.forEach { it.loadMessages() }
    DefaultPreloader.set(Splash::class)

    Application.launch(Simplelauncher::class.java, *args)
}

class Simplelauncher: FxApp(Main::class) {

    /**
     * Register the @Inject services explicitly.
     *
     * FxApp also scans the application package, but that walks .class resources on
     * the classpath, which a GraalVM native image does not have. Registering here
     * keeps both the JVM and the native image working; the scan simply finds these
     * already created.
     */
    override fun beforeInitialization(context: SimpleDiContainer, environment: Environment) {
        context.create(LinkService::class)
        context.create(LinkExecutor::class)
    }

    override fun onStart(command: CommandLine) {
        // set favicon
        Stages.defaultIcons.add("/image/icon/favicon.png")

        DefaultPreloader.notifyMessage("Preparing...")

        // set i18n
        environment.get<String>("simplelauncher.locale").runIfNotEmpty { locale ->
            Locale.setDefault(Locale.forLanguageTag(locale))
        }

        ExposedHelper.connectDatabase()
        logger.debug { ">> database prepared" }
    }

    override fun onStart(stage: Stage) {
        stage.initStyle(StageStyle.EXTENDED)
    }

}
