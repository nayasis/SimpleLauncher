package com.github.nayasis.kotlin.javafx.preloader

import javafx.stage.Stage
import javafx.application.Preloader as FxPreloader

open abstract class NPreloader: FxPreloader() {

    var stage: Stage? = null
    var handler: ((message: String?, percentage: Double?) -> Unit)? = null

    override fun handleApplicationNotification(info: PreloaderNotification?) {
        if( info !is Notificator) return
        if( info.close == true ) {
            stage?.close()
            stage?.scene = null
        } else {
            handler?.let { it(info.message,info.progress) }
        }
    }

    fun close() = notifyPreloader( Notificator(close=true) )

    fun notify(index: Number, max: Number) = notifyPreloader( Notificator().progress(index,max) )

    fun notify(percent: Double, message: String? = null) =
        notifyPreloader( Notificator(progress=percent, message=message) )

    fun notify(message: String) = notifyPreloader( Notificator(message = message) )

}