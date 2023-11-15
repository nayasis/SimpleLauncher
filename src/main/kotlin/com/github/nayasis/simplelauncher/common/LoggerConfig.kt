package com.github.nayasis.simplelauncher.common

import ch.qos.logback.classic.Logger
import com.github.nayasis.kotlin.javafx.app.Environment
import org.slf4j.LoggerFactory

class LoggerConfig(
    private val environment: Environment,
) {
    fun initialize() {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        logger.iteratorForAppenders().forEach { println(">> appender: ${it.name}") }
        println(">> end logger setting")
    }
}