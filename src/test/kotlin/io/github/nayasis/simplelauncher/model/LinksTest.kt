package io.github.nayasis.simplelauncher.model

import io.github.nayasis.kotlin.basica.reflection.Reflector
import io.github.nayasis.simplelauncher.common.ExposedHelper
import io.github.nayasis.simplelauncher.common.ExposedHelper.tx
import io.github.nayasis.simplelauncher.common.ICON_NEW
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Test

private val logger = KotlinLogging.logger {}

class LinksTest {

    @Test
    fun basic() {

        ExposedHelper.connectDatabase(
            url      = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            user     = "user",
            password = "1234",
        )

        tx {

            val created = Link(
                title   = "test link",
                group   = "grp1",
                hashtag = "1,2,3,4",
                icon    = ICON_NEW,
            ).also {
                logger.debug { ">> title: ${it.title}" }
                logger.debug { ">> id   : ${it.id}" }
            }

            val inserted = LinkTable.repo.save(created)

            logger.debug { ">> committed" }
            logger.debug { ">> inserted: $inserted" }

            val read = LinkTable.repo.findById(inserted.id)

            logger.debug { ">> read: $read" }

        }

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