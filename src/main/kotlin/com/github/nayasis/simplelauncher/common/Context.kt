package com.github.nayasis.simplelauncher.common

import com.github.nayasis.simplelauncher.service.ConfigService
import tornadofx.FX
import kotlin.reflect.KClass

class Context { companion object {

    fun <T:Any> bean(klass: KClass<T>): T {
        return FX.dicontainer!!.getInstance(klass)
    }

    val configService: ConfigService by lazy { bean(ConfigService::class)}

}}