package io.github.nayasis.simplelauncher.model

import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.basica.reflection.Reflector
import io.github.nayasis.simplelauncher.common.ICON_NEW
import io.github.nayasis.simplelauncher.common.database
import io.github.nayasis.simplelauncher.common.runQuery
import io.github.nayasis.simplelauncher.database.DataSource.db
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Test
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.firstOrNull
import org.komapper.jdbc.JdbcDatabase

private val logger = KotlinLogging.logger {}

class LinksTest {

    @Test
    fun basic() {

        database = runCatching { JdbcDatabase(
            url      = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            user     = "user",
            password = "1234",
        )}.onFailure { logger.error(it) }.getOrThrow()

        QueryDsl.create(Meta.link).runQuery()

        val created = Link(
            title   = "test link",
            group   = "grp1",
            hashtag = "1,2,3,4",
            icon    = ICON_NEW,
        ).also {
            logger.debug { ">> title: ${it.title}" }
            logger.debug { ">> id   : ${it.id}" }
        }

        val inserted = QueryDsl.insert(Meta.link).single(created).runQuery()

        logger.debug { ">> committed" }
        logger.debug { ">> inserted: $inserted" }

        val read = db.runQuery(QueryDsl.from(Meta.link).where { Meta.link.id eq inserted.id }.firstOrNull())

        logger.debug { ">> read: $read" }

    }

    @Test
    fun toJson() {
        val list = listOf(
            Link(1, "A", icon = ICON_NEW),
            Link(2, "B", icon = ICON_NEW),
            Link(3, "C", icon = ICON_NEW),
            Link(4, "D", icon = ICON_NEW),
        )
        val json = Reflector.toJson(list, pretty = true)
        println(json)
    }

}