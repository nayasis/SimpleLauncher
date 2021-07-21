package com.github.nayasis.simplelauncher

import com.github.nayasis.kotlin.javafx.stage.loadDefaultIcon
import com.github.nayasis.kotlin.spring.javafx.app.SpringFxApp
import com.github.nayasis.simplelauncher.view.Main
import javafx.stage.Stage
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
class Simplelauncher: SpringFxApp(Main::class) {

    override fun start(command: CommandLine) {
        closePreloader()
    }

    override fun start(stage: Stage) {
        stage.apply {
            loadDefaultIcon()
            super.start(this)
        }
    }

    override fun setOptions(options: Options) {}

}

fun main(args: Array<String>) {

    SpringFxApp.loadMessage("/message/**.prop")
    SpringFxApp.loadDefaultIcon("/image/icon/favicon.ico")
//    SpringFxApp.setPreloader(Splash::class)

    launch<Simplelauncher>(*args)

}