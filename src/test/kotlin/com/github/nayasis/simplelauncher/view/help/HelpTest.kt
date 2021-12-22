package com.github.nayasis.simplelauncher.view.help

import com.github.nayasis.kotlin.javafx.spring.SpringFxApp
import com.github.nayasis.simplelauncher.view.Help
import tornadofx.App
import tornadofx.launch

fun main(args: Array<String>) {
    SpringFxApp.loadMessage("/message/**.prop")
    launch<HelpTest>()
}

class HelpTest: App(Help::class)