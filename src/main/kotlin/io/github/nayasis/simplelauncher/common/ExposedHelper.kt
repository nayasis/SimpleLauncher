package io.github.nayasis.simplelauncher.common

import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.javafx.app.FxApp.Companion.environment
import io.github.nayasis.simplelauncher.model.LinkTable
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager

private val logger = KotlinLogging.logger {}

object ExposedHelper {

    lateinit var database: Database

    fun connectDatabase(
        url             : String? = null,
        user            : String? = null,
        password        : String? = null,
    ) {
        database = runCatching { Database.connect(
            url      = url      ?: environment["simplelauncher.datasource.url"]      ?: "",
            user     = user     ?: environment["simplelauncher.datasource.user"]     ?: "",
            password = password ?: environment["simplelauncher.datasource.password"] ?: "",
        ) }.onFailure { logger.error(it) }.getOrThrow()

        transaction {
            SchemaUtils.create(LinkTable)
        }

    }

    fun <T> transaction(
        transactionIsolation: Int = database.transactionManager.defaultIsolationLevel,
        readOnly: Boolean = database.transactionManager.defaultReadOnly,
        block: Transaction.() -> T
    ): T {
        return transaction(transactionIsolation, readOnly, database, block)
    }

}