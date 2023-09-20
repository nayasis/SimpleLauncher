package com.github.nayasis.simplelauncher.model

import com.github.nayasis.kotlin.javafx.misc.toImage
import com.github.nayasis.simplelauncher.common.ICON_NEW
import mu.KotlinLogging
import org.h2.Driver
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
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

            val created = Link.new {
                title = "test link"
                group = "grp1"
//                hashtag = linkedSetOf("1","2","3","4")
                icon = ICON_NEW.toImage()
            }
            logger.debug { ">> title: ${created.title}" }
            logger.debug { ">> id: ${created.id._value}" }
            logger.debug { ">> is new ? : ${created.isNew}" }
            commit()
            logger.debug { ">> committed" }
            logger.debug { ">> inserted: $created" }
            val read = Link.findById(created.id)!!
            logger.debug { ">> read: $read" }
        }
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