package io.github.nayasis.simplelauncher

import io.github.nayasis.kotlin.basica.core.extension.runIfNotEmpty
import io.github.nayasis.kotlin.basica.model.Messages.Companion.loadMessages
import io.github.nayasis.kotlin.basica.net.Networks
import io.github.nayasis.kotlin.swing.app.Environment
import io.github.nayasis.kotlin.swing.app.SwingApp
import io.github.nayasis.kotlin.swing.app.di.SimpleDiContainer
import io.github.nayasis.kotlin.swing.preloader.DefaultPreloader
import io.github.nayasis.kotlin.swing.window.Windows
import io.github.nayasis.simplelauncher.common.Context
import io.github.nayasis.simplelauncher.common.ExposedHelper
import io.github.nayasis.simplelauncher.service.LinkExecutor
import io.github.nayasis.simplelauncher.service.LinkService
import io.github.nayasis.simplelauncher.view.AppTheme
import io.github.nayasis.simplelauncher.view.Main
import io.github.nayasis.simplelauncher.view.Splash
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
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

    Networks.ignoreCerts()
    MESSAGE_BUNDLES.forEach { it.loadMessages() }
    DefaultPreloader.set(Splash::class)

    SwingApp.launch(Simplelauncher::class, *args)
}

class Simplelauncher: SwingApp(Main::class) {

    override fun setupLookAndFeel() {
        AppTheme.of(Context.config.theme).install()
    }

    /**
     * Register the @Inject services explicitly.
     *
     * SwingApp also scans the application package, but that walks .class resources on
     * the classpath, which a GraalVM native image does not have. Registering here
     * keeps both the JVM and the native image working; the scan simply finds these
     * already created.
     */
    override fun beforeInitialization(context: SimpleDiContainer, environment: Environment) {
        context.create(LinkService::class)
        context.create(LinkExecutor::class)
    }

    /**
     * Prepare the data before the main window is built.
     *
     * This runs on the launching thread while the splash lives on the event dispatch
     * thread, so the splash keeps repainting the loading progress.
     */
    override fun afterInitialization(context: SimpleDiContainer, environment: Environment) {
        Windows.defaultIcons.add("/image/icon/favicon.png")

        // set i18n
        environment.get<String>("simplelauncher.locale").runIfNotEmpty { locale ->
            Locale.setDefault(Locale.forLanguageTag(locale))
        }

        DefaultPreloader.notifyMessage("Preparing...")
        ExposedHelper.connectDatabase()
        logger.debug { ">> database prepared" }

        runBlocking {
            val linkService = Context.linkService
            val total = linkService.countAll()
            linkService.loadAll { i, link ->
                DefaultPreloader.notifyProgress(i, total, link.title)
            }
        }
    }

}
