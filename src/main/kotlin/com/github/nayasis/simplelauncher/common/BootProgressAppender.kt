package com.github.nayasis.simplelauncher.common

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.github.nayasis.kotlin.javafx.spring.SpringFxApp

class BootProgressAppender: AppenderBase<ILoggingEvent>() {

    companion object {
        var i = 0
    }

    override fun append(event: ILoggingEvent) {
        SpringFxApp.notifyProgress(++i,8,event.formattedMessage)
    }

}