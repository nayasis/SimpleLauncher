package io.github.nayasis.simplelauncher.model.exposed

import io.github.nayasis.simplelauncher.model.exposed.entity.ExpDepartment
import io.github.nayasis.simplelauncher.model.exposed.entity.ExpDepartmentTable
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private val logger = KotlinLogging.logger {}

class ExpDepartmentDaoTest {

    @Test
    fun writeAndRead() {
        val database = Database.connect(
            url = "jdbc:h2:mem:exp-department;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "user",
            password = "1234",
        )

        transaction(database) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(ExpDepartmentTable)

            val inserted = ExpDepartment.insert(
                tenantId = "tenant-a",
                deptId = 100,
                name = "Department",
                person = """{"name":"Person","age":30}""",
            )

            logger.debug { ">> inserted $inserted" }

            val read = ExpDepartment.find(
                tenantId = inserted.tenantId,
                deptId = inserted.deptId,
            )

            logger.debug { ">> read $read" }

            assertEquals(inserted, read)
        }
    }
}

