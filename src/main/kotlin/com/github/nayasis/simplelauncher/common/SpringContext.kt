package com.github.nayasis.simplelauncher.common

import tornadofx.FX
import kotlin.reflect.KClass

class Context { companion object {

    private fun <T:Any> bean(klass: KClass<T>): T {
        return FX.dicontainer!!.getInstance(klass)
    }

}}