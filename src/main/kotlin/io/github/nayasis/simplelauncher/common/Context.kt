package io.github.nayasis.simplelauncher.common

import io.github.nayasis.kotlin.javafx.app.Environment
import io.github.nayasis.kotlin.javafx.app.FxApp
import io.github.nayasis.simplelauncher.service.Config
import io.github.nayasis.simplelauncher.service.LinkExecutor
import io.github.nayasis.simplelauncher.service.LinkService
import io.github.nayasis.simplelauncher.view.Main
import kotlin.reflect.KClass

class Context { companion object {

    fun <T: Any> bean(klass: KClass<T>): T = FxApp.ctx.get(klass)
        ?: throw AssertionError("No bean ($klass) found in container")

    val config        by lazy { Config.load()}
    val linkService   by lazy { bean(LinkService::class) }
    val linkExecutor  by lazy { bean(LinkExecutor::class) }
    val main          by lazy { FxApp.find(Main::class) }
    val environment   by lazy { bean(Environment::class) }

}}
