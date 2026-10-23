package io.github.nayasis.simplelauncher.common

import io.github.nayasis.kotlin.basica.core.io.invariantPath
import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.swing.app.SwingApp.Companion.environment
import io.github.nayasis.simplelauncher.model.LinkTable
import io.github.oshai.kotlinlogging.KotlinLogging
import org.h2.jdbcx.JdbcConnectionPool
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager


private val logger = KotlinLogging.logger {}

object ExposedHelper {

    lateinit var database: Database

    fun connectDatabase(
        url             : String? = null,
        user            : String? = null,
        password        : String? = null,
    ) {
        database = runCatching {
            // pooled: H2 would otherwise open, and close, the database file for every transaction
            val pool = JdbcConnectionPool.create(
                resolveUrl(url ?: environment["simplelauncher.datasource.url"] ?: ""),
                user     ?: environment["simplelauncher.datasource.user"]     ?: "",
                password ?: environment["simplelauncher.datasource.password"] ?: "",
            )
            Database.connect(pool)
        }.onFailure { logger.error(it) }.getOrThrow()

        tx {
            // the table was named TB_LINK_TEST up to 0.2.0
            if(!LinkTable.exists()) exec("ALTER TABLE IF EXISTS TB_LINK_TEST RENAME TO ${LinkTable.tableName}")
            SchemaUtils.create(LinkTable)
        }

    }

    /** a file database given relative to the working directory (`./`) goes to the [applicationRoot]. */
    private fun resolveUrl(url: String): String {
        val prefix = "jdbc:h2:file:./"
        return if (url.startsWith(prefix)) "jdbc:h2:file:${applicationRoot.invariantPath}/${url.removePrefix(prefix)}" else url
    }

    fun <T> tx(
        database: Database = ExposedHelper.database,
        transactionIsolation: Int = database.transactionManager.defaultIsolationLevel,
        readOnly: Boolean = database.transactionManager.defaultReadOnly,
        block: JdbcTransaction.() -> T
    ): T {
        return transaction(database, transactionIsolation, readOnly, block)
    }

}