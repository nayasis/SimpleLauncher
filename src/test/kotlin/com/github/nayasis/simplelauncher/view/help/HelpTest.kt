package com.github.nayasis.simplelauncher.view.help

import com.github.nayasis.kotlin.basica.model.Messages
import com.github.nayasis.simplelauncher.view.Help
import tornadofx.App
import tornadofx.launch

fun main(args: Array<String>) {
    Messages.loadFromResource("/message/**.prop")
    launch<HelpTest>()
}

class HelpTest: App(Help::class)