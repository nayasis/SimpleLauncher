package com.github.nayasis.kotlin.spring.javafx.app

import com.github.nayasis.kotlin.basica.model.Messages
import com.github.nayasis.kotlin.javafx.preloader.NPreloader
import com.github.nayasis.kotlin.javafx.stage.DEFAULT_ICON
import javafx.scene.image.Image
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

fun setPreloader(preloader: KClass<out NPreloader>) {
    System.setProperty("javafx.preloader", preloader.jvmName)
    System.setProperty("java.awt.headless", "false")
}

fun loadDefaultIcon(resourcePath: String) = DEFAULT_ICON.add(resourcePath)

fun loadMessage(resourcePath: String) = Messages.loadFromResource(resourcePath)

@Suppress("SpringJavaConstructorAutowiringInspection")
open class TornadoFxSpringApp: App {

    constructor(primaryView: KClass<out UIComponent> = NoPrimaryViewSpecified::class, vararg stylesheet: KClass<out Stylesheet>) : super(primaryView, *stylesheet)
    constructor(primaryView: KClass<out UIComponent> = NoPrimaryViewSpecified::class, stylesheet: KClass<out Stylesheet>, scope: Scope = FX.defaultScope) : super(primaryView, stylesheet)
    constructor(icon: Image, primaryView: KClass<out UIComponent> = NoPrimaryViewSpecified::class, vararg stylesheet: KClass<out Stylesheet>) : super(icon, primaryView, *stylesheet)

    lateinit var context: ConfigurableApplicationContext

    override fun init() {
        context = SpringApplication.run(this.javaClass)
        context.autowireCapableBeanFactory.autowireBean(this)
        FX.dicontainer = object: DIContainer {
            override fun <T: Any> getInstance(type: KClass<T>): T = context.getBean(type.java)
            override fun <T: Any> getInstance(type: KClass<T>, name: String): T = context.getBean(name, type.java)
        }
    }

    override fun stop() {
        super.stop()
        context.close()
    }

}