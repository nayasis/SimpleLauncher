package com.github.nayasis.kotlin.javafx.preloader

import javafx.application.Preloader
import java.lang.Double.isInfinite
import java.lang.Double.isNaN

class Notificator(
    var message: String? = null,
    var progress: Double? = null,
    val close: Boolean? = null,
): Preloader.PreloaderNotification {

    fun progress(index: Number, max: Number): Notificator {
        progress(index.toDouble(), max.toDouble())
        return this
    }

    private fun progress(index: Double, max: Double) {
        progress = when {
            isInvalid(index) -> 0.0
            isInvalid(max) -> 0.0
            else -> {
                if(index > max) max else index / max
            }
        }
    }

    private fun isInvalid(number: Double): Boolean {
        return isInfinite(number) || isNaN(number) || number < 0
    }

}