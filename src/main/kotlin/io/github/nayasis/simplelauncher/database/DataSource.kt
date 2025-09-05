package io.github.nayasis.simplelauncher.database

import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.javafx.app.FxApp.Companion.environment
import io.github.oshai.kotlinlogging.KotlinLogging
import org.komapper.jdbc.JdbcDatabase

private val logger = KotlinLogging.logger {}

object DataSource {

    val db = runCatching { JdbcDatabase(
        url      = environment["simplelauncher.datasource.url"] ?: "",
        user     = environment["simplelauncher.datasource.user"] ?: "",
        password = environment["simplelauncher.datasource.password"] ?: "",
    ) }.onFailure { logger.error(it) }.getOrThrow()

}