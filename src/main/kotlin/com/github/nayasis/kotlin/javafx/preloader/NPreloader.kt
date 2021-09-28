package com.github.nayasis.kotlin.javafx.preloader

import com.github.nayasis.kotlin.javafx.stage.Dialog
import javafx.application.Platform
import javafx.application.Preloader as FxPreloader

abstract class NPreloader: FxPreloader() {

    override fun handleApplicationNotification(notificator: PreloaderNotification) {
        when(notificator) {
            is ProgressNotificator -> onProgress(notificator)
            is ErrorNotificator -> onError(notificator)
            is CloseNotificator -> onClose(notificator)
        }
    }

    open fun onError( notificator: ErrorNotificator ) {
        Platform.runLater {
            notificator.throwable?.printStackTrace( System.err )
            with(notificator) {
                Dialog.error( message, throwable )
            }
            onClose()
        }
    }

    abstract fun onProgress( notificator: ProgressNotificator )

    abstract fun onClose( notificator: CloseNotificator = CloseNotificator() )

}


