package io.github.nayasis.simplelauncher.model

import io.github.nayasis.kotlin.basica.reflection.Reflector
import io.github.nayasis.kotlin.basica.reflection.toObject
import io.github.nayasis.simplelauncher.common.ExposedHelper
import io.github.nayasis.simplelauncher.common.ExposedHelper.tx
import io.github.nayasis.simplelauncher.common.ICON_NEW
import io.github.nayasis.simplelauncher.model.vo.JsonLink
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import mslinks.ShellLink
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.nio.ByteBuffer
import java.util.Base64

private val logger = KotlinLogging.logger {}

class LinksTest {

    @TempDir
    lateinit var dir: File

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
                group   = hashSetOf("grp1", "tool"),
                hashtag = hashSetOf("1", "2", "3", "4"),
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

            read?.group shouldBe hashSetOf("grp1", "tool")
            read?.groupJson shouldBe """["grp1","tool"]"""
            read?.hashtag shouldBe hashSetOf("1", "2", "3", "4")
            read?.hashtagJson shouldBe """["1","2","3","4"]"""

        }

    }

    @Test
    fun tableOfTheOldNameIsRenamed() {
        val url = "jdbc:h2:mem:rename;DB_CLOSE_DELAY=-1"
        ExposedHelper.connectDatabase(url = url, user = "user", password = "1234")
        tx {
            LinkTable.repo.save(Link(title = "kept"))
            exec("ALTER TABLE ${LinkTable.tableName} RENAME TO TB_LINK_TEST")
        }

        ExposedHelper.connectDatabase(url = url, user = "user", password = "1234")

        tx(readOnly = true) { LinkTable.repo.selectAll().map { it.title } } shouldBe listOf("kept")
    }

    @Test
    fun restoreFromTakesEveryStoredFieldButTheId() {
        val stored = Link(7, "A", group = hashSetOf("g"), path = "a.exe", argument = "-x", hashtag = hashSetOf("h"))
        val edited = stored.clone().apply { id = 7; title = "B"; group = hashSetOf("x"); path = null; argument = null }

        edited.restoreFrom(stored.apply { id = 99 })

        edited.id shouldBe 7
        edited.title shouldBe "A"
        edited.group shouldBe hashSetOf("g")
        edited.groupJson shouldBe stored.groupJson
        edited.path shouldBe "a.exe"
        edited.argument shouldBe "-x"
    }

    @Test
    fun exportedLinksImportBack() {
        val links = listOf(
            Link(1, "A", group = hashSetOf("tool"), path = "c:/app/a.exe", icon = ICON_NEW),
            Link(2, "B", hashtag = hashSetOf("dev")),
        )

        val json = Reflector.toJson(links.map { JsonLink(it) }, pretty = true)
        val read = json.toObject<List<JsonLink>>().map { it.toLink() }

        read.map { it.title } shouldBe listOf("A", "B")
        read[0].group shouldBe hashSetOf("tool")
        read[0].path shouldBe "c:/app/a.exe"
        read[0].icon?.toList() shouldBe ICON_NEW.toList()
        read[1].hashtag shouldBe hashSetOf("dev")
        read[1].icon.shouldBeNull()
    }

    @Test
    fun iconIsExportedAsPlainBase64() {
        val icon = JsonLink(Link(icon = ICON_NEW)).icon!!

        // the PNG file itself, not a serialized Java object
        Base64.getDecoder().decode(icon).toList() shouldBe ICON_NEW.toList()
        icon shouldStartWith "iVBORw0KGgo"
    }

    @Test
    fun notBase64IconIsDropped() {
        JsonLink(title = "A", icon = "not base64 !").toLink().icon.shouldBeNull()
    }

    @Test
    fun legacySerializedIconImportsAsImageBytes() {
        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(ICON_NEW) }
            output.toByteArray()
        }

        val link = JsonLink(icon = Base64.getEncoder().encodeToString(bytes)).toLink()

        link.icon?.toList() shouldBe ICON_NEW.toList()
        link.iconImage.shouldNotBeNull()
        JsonLink(link).icon shouldStartWith "iVBORw0KGgo"
    }

    @Test
    fun malformedLegacyIconIsDropped() {
        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(ICON_NEW) }
            output.toByteArray()
        }
        val wrongLength = bytes.clone().apply { ByteBuffer.wrap(this, 23, 4).putInt(Int.MAX_VALUE) }

        listOf(bytes.copyOf(26), wrongLength, bytes + byteArrayOf(0)).forEach { malformed ->
            JsonLink(icon = Base64.getEncoder().encodeToString(malformed)).toLink().icon.shouldBeNull()
        }
    }

    @Test
    fun existingPathIsUsed() {
        val file = File(dir, "a.exe").apply { writeText("") }

        Link(path = file.path).toPath() shouldBe file.toPath()
    }

    @Test
    fun relativePathIsUsedWhenThePathIsGone() {
        val file = File(dir, "a.exe").apply { writeText("") }
        val link = Link(path = File(dir, "gone/a.exe").path, relativePath = file.absolutePath)

        link.toPath() shouldBe file.toPath()
        link.path shouldBe file.invariantSeparatorsPath
    }

    @Test
    fun emptyPathIsNoPath() {
        Link(path = "").toPath().shouldBeNull()
        Link(path = "  ").toPath().shouldBeNull()
    }

    @Test
    fun typedPathResetsRelativePath() {
        val link = Link(path = "c:/old/a.exe", relativePath = "old/a.exe")

        link.changePath("")

        link.path.shouldBeNull()
        link.relativePath.shouldBeNull()
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun shortcutFileTakesItsTarget() {
        val target = File(dir, "tool.exe").apply { writeText("") }
        val lnk = File(dir, "tool.lnk")
        ShellLink.createLink(target.absolutePath, lnk.absolutePath)

        val link = Link(lnk)

        File(link.path!!) shouldBe target.absoluteFile
        link.toPath() shouldBe target.toPath()
    }

}
