package io.github.nayasis.simplelauncher.model

import io.github.nayasis.kotlin.basica.reflection.Reflector
import io.github.nayasis.kotlin.basica.reflection.toObject
import io.github.nayasis.simplelauncher.common.ExposedHelper.tx
import io.github.nayasis.simplelauncher.model.entity.Department
import io.github.nayasis.simplelauncher.model.entity.DepartmentTable
import io.github.nayasis.simplelauncher.model.entity.Person
import io.github.nayasis.simplelauncher.model.entity.repo
import io.github.nayasis.simplelauncher.model.vo.JsonLink
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.junit.jupiter.api.Test

private val logger = KotlinLogging.logger {}

class JsonMappingTest {

    @Test
    fun jsonLinkShouldPreserveExecuteEach() {
        val link = Link(
            title = "test",
            showConsole = true,
            executeEach = false,
        )

        val restored = Reflector
            .toJson(JsonLink(link))
            .toObject<JsonLink>()
            .toLink()

        restored.executeEach shouldBe false
        restored.showConsole shouldBe true
    }

    @Test
    fun jsonLinkShouldExportHashtagAsArray() {
        val link = Link(
            title = "test",
            group = hashSetOf("util", "dev"),
            hashtag = hashSetOf("dev", "tool"),
        )

        val json = Reflector.toJson(JsonLink(link))

        json.contains("\"group\":[") shouldBe true
        json.contains("\"util\"") shouldBe true
        json.contains("\"hashtag\":[") shouldBe true
        json.contains("\"dev\"") shouldBe true
        json.contains("\"tool\"") shouldBe true
    }

    @Test
    fun jsonLinkShouldImportHashtagArray() {
        val restored = """
            {
              "title": "test",
              "group": ["util", "dev", "util", ""],
              "hashtag": ["dev", "tool", "dev", ""]
            }
        """.trimIndent()
            .toObject<JsonLink>()
            .toLink()

        restored.group shouldBe hashSetOf("util", "dev")
        restored.hashtag shouldBe hashSetOf("dev", "tool")
    }

    @Test
    fun jsonLinkShouldImportOldStringGroupAndHashtag() {
        val restored = """
            {
              "title": "test",
              "group": "util dev,script",
              "hashtag": "dev tool,util,,"
            }
        """.trimIndent()
            .toObject<JsonLink>()
            .toLink()

        restored.group shouldBe hashSetOf("util", "dev", "script")
        restored.hashtag shouldBe hashSetOf("dev", "tool", "util")
    }

    @Test
    fun jsonLinkShouldImportPlainTextGroupAsLowercaseTokens() {
        val restored = """
            {
              "title": "test",
              "group": "Dev Tool / Script.Runner  한글-그룹"
            }
        """.trimIndent()
            .toObject<JsonLink>()
            .toLink()

        restored.group shouldBe hashSetOf("dev", "tool", "script", "runner", "한글", "그룹")
        restored.groupJson shouldBe """["dev","tool","script","runner","한글","그룹"]"""
    }

    @Test
    fun writeAndRead() {

        val database = Database.connect(
            url      = "jdbc:h2:mem:department;DB_CLOSE_DELAY=-1",
            driver   = "org.h2.Driver",
            user     = "user",
            password = "1234",
        )

        tx(database) {

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
                DepartmentTable.repo.save(this)
            }

            logger.debug { ">> inserted $inserted" }

            val read = DepartmentTable.repo.select().where {
                (DepartmentTable.tenantId eq inserted.tenantId) and
                (DepartmentTable.deptId eq inserted.deptId)
            }.singleOrNull()

            logger.debug { ">> read $read" }

            inserted shouldBe read

        }
    }

}
