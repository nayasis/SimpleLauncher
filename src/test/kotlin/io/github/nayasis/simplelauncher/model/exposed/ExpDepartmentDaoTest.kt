package io.github.nayasis.simplelauncher.model.exposed

import io.github.nayasis.simplelauncher.model.entity.Person
import io.github.nayasis.simplelauncher.model.exposed.entity.ExpDepartment
import io.github.nayasis.simplelauncher.model.exposed.entity.ExpDepartmentTable
import io.github.nayasis.simplelauncher.model.exposed.entity.repo
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

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

            val inserted = ExpDepartment(
                tenantId = "tenant-a",
                deptId = "100",
                name = "Department",
                person = Person(
                    name = "jake",
                    age = 10,
                    address = "123 Main Street",
                )
            ).apply {
                ExpDepartmentTable.repo.insert(this)
            }

            logger.debug { ">> inserted $inserted" }

            val read = ExpDepartmentTable.repo.select().where {
                (ExpDepartmentTable.tenantId eq inserted.tenantId) and
                (ExpDepartmentTable.deptId eq inserted.deptId)
            }.singleOrNull()

            logger.debug { ">> read $read" }

            assertEquals(inserted, read)
        }
    }
}

