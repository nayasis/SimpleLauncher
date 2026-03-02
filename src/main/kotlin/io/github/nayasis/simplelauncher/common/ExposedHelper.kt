package io.github.nayasis.simplelauncher.common

import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.javafx.app.Environment
import io.github.nayasis.kotlin.javafx.app.FxApp.Companion.environment
import io.github.nayasis.simplelauncher.model.LinkTable
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager


private val logger = KotlinLogging.logger {}

object ExposedHelper {

    lateinit var database: Database

    fun connectDatabase(environment: Environment) {
        connectDatabase(
            environment["simplelauncher.datasource.url"],
            environment["simplelauncher.datasource.user"],
            environment["simplelauncher.datasource.password"],
        )
    }

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

        tx {
            SchemaUtils.create(LinkTable)
        }

    }

    fun <T> tx(
        database: Database = ExposedHelper.database,
        transactionIsolation: Int = database.transactionManager.defaultIsolationLevel,
        readOnly: Boolean = database.transactionManager.defaultReadOnly,
        block: Transaction.() -> T
    ): T {
        return transaction(database, transactionIsolation, readOnly, block)
    }

}