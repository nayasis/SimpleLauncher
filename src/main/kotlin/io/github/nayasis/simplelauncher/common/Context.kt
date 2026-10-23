package io.github.nayasis.simplelauncher.common

import io.github.nayasis.kotlin.swing.app.SwingApp
import io.github.nayasis.simplelauncher.service.Config
import io.github.nayasis.simplelauncher.service.LinkExecutor
import io.github.nayasis.simplelauncher.service.LinkService
import io.github.nayasis.simplelauncher.view.Main
import kotlin.reflect.KClass

class Context { companion object {

    fun <T: Any> bean(klass: KClass<T>): T = SwingApp.ctx.get(klass)
        ?: throw AssertionError("No bean ($klass) found in container")

    val config        by lazy { Config.load()}
    val linkService   by lazy { bean(LinkService::class) }
    val linkExecutor  by lazy { bean(LinkExecutor::class) }
    val main          by lazy { SwingApp.find(Main::class) }

}}
