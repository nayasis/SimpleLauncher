package io.github.nayasis.simplelauncher.model

import io.github.nayasis.kotlin.basica.reflection.Reflector
import io.github.nayasis.kotlin.javafx.misc.toImage
import io.github.nayasis.simplelauncher.common.ICON_NEW
import io.github.oshai.kotlinlogging.KotlinLogging
import org.h2.Driver
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test

private val logger = KotlinLogging.logger {}

class LinksTest {
    @Test
    fun basic() {
        connectDb()
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Links)

            val created = Link(
                title = "test link",
                group = "grp1",
//                hashtag = linkedSetOf("1","2","3","4"),
                icon = ICON_NEW.toImage(),
            )
            logger.debug { ">> title: ${created.title}" }
            logger.debug { ">> id: ${created.id}" }

//            created.id = Links.insert { it.from(created) }[Links.id]

            Links.save(created)

            commit()
            logger.debug { ">> committed" }
            logger.debug { ">> inserted: $created" }
            val read = Links.selectAll().where { Links.id eq created.id }.singleOrNull()?.toLink()
            logger.debug { ">> read: $read" }
        }
    }

    @Test
    fun toJson() {
        val list = listOf(
            Link(1, "A", icon = ICON_NEW.toImage()),
            Link(2, "B", icon = ICON_NEW.toImage()),
            Link(3, "C", icon = ICON_NEW.toImage()),
            Link(4, "D", icon = ICON_NEW.toImage()),
        )
        val json = Reflector.toJson(list, pretty = true)
        println(json)
    }

}

private fun connectDb() {
    Database.connect(
//        url      = "jdbc:mariadb://localhost:3306/sample?rewriteBatchedStatements = true",
        url      = "jdbc:h2:mem:test",
        driver   = Driver::class.qualifiedName!!,
        user     = "user",
        password = "1234"
    )
}