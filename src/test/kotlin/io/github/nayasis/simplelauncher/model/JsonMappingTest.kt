package io.github.nayasis.simplelauncher.model

import io.github.nayasis.simplelauncher.model.entity.Department
import io.github.nayasis.simplelauncher.model.entity.DepartmentTable
import io.github.nayasis.simplelauncher.model.entity.Person
import io.github.nayasis.simplelauncher.model.entity.repo
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

private val logger = KotlinLogging.logger {}

class JsonMappingTest {

    @Test
    fun writeAndRead() {

        val database = Database.connect(
            url      = "jdbc:h2:mem:department;DB_CLOSE_DELAY=-1",
            driver   = "org.h2.Driver",
            user     = "user",
            password = "1234",
        )

        transaction(database) {

            SchemaUtils.create(DepartmentTable)

            val inserted = Department(
                tenantId = "tenant-a",
                deptId   = "100",
                name     = "Department",
                person   = Person(
                    name    = "jake",
                    age     = 10,
                    address = "123 Main Street",
                ),
                attribute = mapOf(
                    "key1" to "value1",
                    "key2" to 123,
                    "key3" to listOf(1, 2, 3),
                ),
            ).apply {
                DepartmentTable.repo.insert(this)
            }

            logger.debug { ">> inserted $inserted" }

            val read = DepartmentTable.repo.select().where {
                (DepartmentTable.tenantId eq inserted.tenantId) and (DepartmentTable.deptId eq inserted.deptId)
            }.singleOrNull()

            logger.debug { ">> read $read" }

            assertEquals(inserted, read)
        }
    }

}