package com.github.nayasis.kotlin.spring.javafx.app

import com.github.nayasis.kotlin.basica.model.Messages
import com.github.nayasis.kotlin.javafx.preloader.CloseNotificator
import com.github.nayasis.kotlin.javafx.preloader.ErrorNotificator
import com.github.nayasis.kotlin.javafx.preloader.NPreloader
import com.github.nayasis.kotlin.javafx.preloader.Notificator
import com.github.nayasis.kotlin.javafx.preloader.ProgressNotificator
import com.github.nayasis.kotlin.javafx.stage.DEFAULT_ICON
import javafx.scene.image.Image
import javafx.stage.Stage
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import tornadofx.App
import tornadofx.DIContainer
import tornadofx.FX
import tornadofx.NoPrimaryViewSpecified
import tornadofx.Scope
import tornadofx.Stylesheet
import tornadofx.UIComponent
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName


@Suppress("SpringJavaConstructorAutowiringInspection")
abstract class TornadoFxSpringApp: App {

    constructor(primaryView: KClass<out UIComponent> = NoPrimaryViewSpecified::class, vararg stylesheet: KClass<out Stylesheet>) : super(primaryView, *stylesheet)
    constructor(primaryView: KClass<out UIComponent> = NoPrimaryViewSpecified::class, stylesheet: KClass<out Stylesheet>, scope: Scope = FX.defaultScope) : super(primaryView, stylesheet)
    constructor(icon: Image, primaryView: KClass<out UIComponent> = NoPrimaryViewSpecified::class, vararg stylesheet: KClass<out Stylesheet>) : super(icon, primaryView, *stylesheet)

    private val options = Options()

    lateinit var context: ConfigurableApplicationContext

    override fun init() {
        try {
            setOptions(options)
            context = SpringApplication.run(this.javaClass, *parameters.raw.toTypedArray())
            context.autowireCapableBeanFactory.autowireBean(this)
            FX.dicontainer = object: DIContainer {
                override fun <T: Any> getInstance(type: KClass<T>): T = context.getBean(type.java)
                override fun <T: Any> getInstance(type: KClass<T>, name: String): T = context.getBean(name, type.java)
            }
        } catch (e: Exception) {
            notifyPreloader(ErrorNotificator("Error on starting",e))
            closePreloader()
            stop()
        }

    }

    override fun start(stage: Stage) {
        start(DefaultParser().parse(options, parameters.raw.toTypedArray()))
        super.start(stage)
    }

    override fun stop() {
        try { context.close() } catch (ignore: Exception) {}
        try { super.stop() } catch (ignore: Exception) {}
    }

    private fun notifyPreloader( notificator: Notificator ) {
        super.notifyPreloader( notificator )
    }

    fun notifyProgress(percent: Double, message: String? = null) {
        notifyPreloader(ProgressNotificator(percent,message))
    }

    fun notifyProgress(index: Number, max: Number, message: String? = null) {
        notifyPreloader(ProgressNotificator(index,max,message))
    }

    fun closePreloader() {
        notifyPreloader(CloseNotificator())
    }

    abstract fun setOptions(options: Options)
    abstract fun start(command: CommandLine)

    companion object {

        fun setPreloader(preloader: KClass<out NPreloader>) {
            System.setProperty("javafx.preloader", preloader.jvmName)
            System.setProperty("java.awt.headless", "false")
        }

        fun loadDefaultIcon(resourcePath: String) = DEFAULT_ICON.add(resourcePath)

        fun loadMessage(resourcePath: String) = Messages.loadFromResource(resourcePath)

    }

}