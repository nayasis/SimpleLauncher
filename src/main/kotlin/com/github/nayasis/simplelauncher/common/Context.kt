package com.github.nayasis.simplelauncher.common

import com.github.nayasis.kotlin.basica.core.extention.ifEmpty
import com.github.nayasis.kotlin.javafx.spring.SpringFxApp.Companion.environment
import com.github.nayasis.simplelauncher.service.ConfigService
import com.github.nayasis.simplelauncher.service.LinkExecutor
import com.github.nayasis.simplelauncher.service.LinkService
import com.github.nayasis.simplelauncher.view.Help
import com.github.nayasis.simplelauncher.view.Main
import org.springframework.core.env.get
import tornadofx.FX
import tornadofx.find
import kotlin.reflect.KClass

class Context { companion object {

    fun <T:Any> bean(klass: KClass<T>): T = FX.dicontainer!!.getInstance(klass)

    fun environment(key: String, default: String = ""): String = environment[key].ifEmpty { default }

    val configService by lazy { bean(ConfigService::class)}
    val linkService   by lazy { bean(LinkService::class)}
    val linkExecutor  by lazy { bean(LinkExecutor::class)}
    val main          by lazy { find(Main::class) }
    val help          by lazy { find(Help::class) }

}}