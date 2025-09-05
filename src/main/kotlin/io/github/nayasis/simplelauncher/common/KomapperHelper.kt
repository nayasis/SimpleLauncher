package io.github.nayasis.simplelauncher.common

import org.komapper.core.dsl.query.EntityInsertSingleQuery
import org.komapper.core.dsl.query.Query
import org.komapper.core.dsl.query.SchemaCreateQuery
import org.komapper.jdbc.JdbcDatabase

var database: JdbcDatabase? = null

fun SchemaCreateQuery.runQuery() {
    database?.runQuery(this)
        ?: throw IllegalStateException("Database is not initialized")
}

fun <T: Any> EntityInsertSingleQuery<T>.runQuery(): T {
    return database?.runQuery(this)
        ?: throw IllegalStateException("Database is not initialized")
}

fun <T: Any> Query<out T>.runQuery(): T {
    return database?.runQuery(this)
        ?: throw IllegalStateException("Database is not initialized")
}