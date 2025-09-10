package io.github.nayasis.simplelauncher.common

import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.javafx.app.FxApp.Companion.environment
import io.github.oshai.kotlinlogging.KotlinLogging
import org.komapper.core.dsl.query.Query
import org.komapper.jdbc.JdbcDatabase
import org.komapper.tx.core.EmptyTransactionProperty
import org.komapper.tx.core.TransactionAttribute
import org.komapper.tx.core.TransactionOperator
import org.komapper.tx.core.TransactionProperty

private val logger = KotlinLogging.logger {}

object KomapperHelper {

    lateinit var database: JdbcDatabase

    fun connectDatabase() {
        database = runCatching { JdbcDatabase(
            url      = environment["simplelauncher.datasource.url"] ?: "",
            user     = environment["simplelauncher.datasource.user"] ?: "",
            password = environment["simplelauncher.datasource.password"] ?: "",
        ) }.onFailure { logger.error(it) }.getOrThrow()
    }

    fun <T> Query<T>.runQuery(): T {
        return database.runQuery(this)
    }

    fun <R> withTransaction(
        transactionAttribute: TransactionAttribute = TransactionAttribute.REQUIRED,
        transactionProperty: TransactionProperty = EmptyTransactionProperty,
        block: (TransactionOperator) -> R
    ): R {
        return database.withTransaction { block(it) }
    }

}
