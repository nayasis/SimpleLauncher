package com.github.nayasis.simplelauncher.common

import com.github.nayasis.simplelauncher.service.ConfigService
import com.github.nayasis.simplelauncher.service.LinkExecutor
import com.github.nayasis.simplelauncher.service.LinkService
import com.github.nayasis.simplelauncher.view.Main
import tornadofx.FX
import tornadofx.find
import kotlin.reflect.KClass

class Context { companion object {

    fun <T:Any> bean(klass: KClass<T>): T = FX.dicontainer!!.getInstance(klass)

    val configService by lazy { bean(ConfigService::class)}
    val linkService   by lazy { bean(LinkService::class)}
    val linkExecutor  by lazy { bean(LinkExecutor::class)}
    val main          by lazy { find(Main::class) }

}}