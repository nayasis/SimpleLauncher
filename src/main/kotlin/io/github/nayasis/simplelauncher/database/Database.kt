package io.github.nayasis.simplelauncher.database

import org.komapper.core.dsl.QueryDsl
import org.komapper.jdbc.JdbcDatabase
import org.komapper.dialect.h2.H2JdbcDialect
import io.github.nayasis.simplelauncher.model.Link

object Database {
    private val database = JdbcDatabase(
        url = "jdbc:h2:file:./data/simplelauncher;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        driver = "org.h2.Driver",
    )

    val linkMeta = org.komapper.core.dsl.Meta.link

    fun <T> transaction(block: (JdbcDatabase) -> T): T {
        return database.withTransaction { block(it) }
    }

    fun <T> query(block: (JdbcDatabase) -> T): T {
        return block(database)
    }
}
