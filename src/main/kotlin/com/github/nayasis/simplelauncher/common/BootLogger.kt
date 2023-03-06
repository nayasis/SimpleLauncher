package com.github.nayasis.simplelauncher.common

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.github.nayasis.kotlin.javafx.spring.SpringFxApp
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

class BootLogger {

    private val captureAppender = BootProgressAppender()
    private val progressLogger: ch.qos.logback.classic.Logger
        get() = LoggerFactory.getLogger("org.springframework") as ch.qos.logback.classic.Logger

    fun getInitializer(): ApplicationContextInitializer<ConfigurableApplicationContext> {
        return ApplicationContextInitializer<ConfigurableApplicationContext> {
            captureAppender.start()
            progressLogger.apply {
                addAppender(captureAppender)
            }
        }
    }

    fun close() {
        captureAppender.stop()
        progressLogger.detachAppender(captureAppender)
    }

}

class BootProgressAppender: AppenderBase<ILoggingEvent>() {
    var i = 0
    override fun append(event: ILoggingEvent) {
        val rate = 1.0 * ++i / 6 * 0.9
        SpringFxApp.notifyProgress(rate, event.formattedMessage)
    }
    override fun getName(): String = "capture"
}