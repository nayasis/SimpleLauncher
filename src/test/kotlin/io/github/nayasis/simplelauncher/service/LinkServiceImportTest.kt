package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.reflection.toObject
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.github.nayasis.simplelauncher.common.ExposedHelper
import io.github.nayasis.simplelauncher.common.ExposedHelper.tx
import io.github.nayasis.simplelauncher.common.ICON_NEW
import io.github.nayasis.simplelauncher.model.Link
import io.github.nayasis.simplelauncher.model.LinkTable
import io.github.nayasis.simplelauncher.model.repo
import io.github.nayasis.simplelauncher.model.vo.JsonLink
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class LinkServiceImportTest {

    @TempDir
    lateinit var directory: Path

    @Test
    fun legacyFileIconsSurviveImportReloadAndExport() = runBlocking<Unit> {
        connectDatabase("legacy-file")
        tx { LinkTable.repo.save(Link(title = "Existing", icon = ICON_NEW)) }
        val service = LinkService()
        service.loadAll()

        val file = Path.of(javaClass.getResource("/config/legacy-icons.json")!!.toURI())
        service.importData(file)
        service.loadAll()

        assertLegacyLinks(service)

        val reloaded = LinkService()
        reloaded.loadAll()
        assertLegacyLinks(reloaded)

        val exported = directory.resolve("exported.json")
        reloaded.exportData(exported)
        val exportedLinks = Files.readString(exported).toObject<List<JsonLink>>().map { it.toLink() }
        val expectedIcons = runUiAndWait { reloaded.links.associate { it.title to it.icon } }
        exportedLinks.forEach { assertContentEquals(expectedIcons[it.title], it.icon) }

        connectDatabase("legacy-round-trip")
        val importedAgain = LinkService()
        importedAgain.importData(exported)
        importedAgain.loadAll()
        assertLegacyLinks(importedAgain)
    }

    @Test
    fun storedLegacyIconsLoadWithoutReimport() = runBlocking<Unit> {
        connectDatabase("stored-legacy-icon")
        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(ICON_NEW) }
            output.toByteArray()
        }
        tx { LinkTable.repo.save(Link(title = "Stored legacy", icon = bytes)) }

        val service = LinkService()
        service.loadAll()

        runUiAndWait<Unit> {
            val link = service.links.single()
            assertNotNull(link.iconImage)
            assertContentEquals(bytes, link.icon)
        }
    }

    private fun assertLegacyLinks(service: LinkService) = runUiAndWait<Unit> {
        assertEquals(4, service.links.size)
        assertEquals(3, service.links.count { it.iconImage != null })
        val legacy = service.links.single { it.title == "Legacy executable" }
        assertEquals(hashSetOf("utility", "tools"), legacy.group)
        assertEquals(hashSetOf("legacy", "icon"), legacy.hashtag)
        assertEquals("missing/legacy.exe", legacy.path)
        assertEquals("-legacy", legacy.argument)
        assertFalse(legacy.executeEach)
        assertNull(service.links.single { it.title == "Without icon" }.icon)
        assertContentEquals(ICON_NEW, service.links.single { it.title == "Existing" }.icon)
    }

    private fun connectDatabase(name: String) {
        ExposedHelper.connectDatabase("jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1", "user", "1234")
    }
}
