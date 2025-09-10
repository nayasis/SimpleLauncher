package io.github.nayasis.simplelauncher.common

import org.komapper.core.dsl.query.Query
import org.komapper.jdbc.JdbcDatabase
import org.komapper.tx.core.EmptyTransactionProperty
import org.komapper.tx.core.TransactionAttribute
import org.komapper.tx.core.TransactionOperator
import org.komapper.tx.core.TransactionProperty

var defaultDatabase: JdbcDatabase? = null

fun <T> Query<T>.runQuery(database: JdbcDatabase? = null): T {
    return getDatabase(database).runQuery(this)
}

fun <R> withTransaction(
    database: JdbcDatabase? = null,
    transactionAttribute: TransactionAttribute = TransactionAttribute.REQUIRED,
    transactionProperty: TransactionProperty = EmptyTransactionProperty,
    block: (TransactionOperator) -> R
): R {
    return getDatabase(database).withTransaction { block(it) }
}



private fun getDatabase(database: JdbcDatabase?): JdbcDatabase {
    return (database ?: defaultDatabase)
        ?: throw IllegalStateException("Database is not initialized")
}

