package com.github.nayasis.simplelauncher

import com.github.nayasis.kotlin.spring.javafx.app.TornadoFxSpringApp
import com.github.nayasis.simplelauncher.view.Preloader
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Options
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.stereotype.Component
import tornadofx.View
import tornadofx.label
import tornadofx.launch
import tornadofx.paddingAll
import tornadofx.vbox

@SpringBootApplication
class Simplelauncher: TornadoFxSpringApp(SpringExampleView::class) {

    override fun start(command: CommandLine) {
        closePreloader()
    }

    override fun setOptions(options: Options) {}

}

fun main(args: Array<String>) {

    TornadoFxSpringApp.loadMessage("/message/**.prop")
    TornadoFxSpringApp.loadDefaultIcon("/image/icon/favicon.ico")
    TornadoFxSpringApp.setPreloader(Preloader::class)

    launch<Simplelauncher>(*args)

}

class SpringExampleView: View("Example View") {

    val bean: HelloBean by di()

    override val root = vbox{
        label(bean.helloworld()).paddingAll = 20
    }

}

@Component
class HelloBean {
    fun helloworld(): String = "Hello by di()"
}