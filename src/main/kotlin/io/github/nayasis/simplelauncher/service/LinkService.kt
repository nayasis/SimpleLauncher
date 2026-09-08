package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.core.io.directory
import io.github.nayasis.kotlin.basica.core.io.notExists
import io.github.nayasis.kotlin.basica.core.io.pathString
import io.github.nayasis.kotlin.basica.core.io.readText
import io.github.nayasis.kotlin.basica.core.io.writeText
import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.kotlin.basica.core.string.toPath
import io.github.nayasis.kotlin.basica.reflection.Reflector
import io.github.nayasis.kotlin.basica.reflection.toObject
import io.github.nayasis.kotlin.javafx.app.di.Inject
import io.github.nayasis.kotlin.javafx.misc.Desktop
import io.github.nayasis.kotlin.javafx.misc.set
import io.github.nayasis.kotlin.javafx.stage.Dialog
import io.github.nayasis.simplelauncher.common.Context.Companion.config
import io.github.nayasis.simplelauncher.common.Context.Companion.main
import io.github.nayasis.simplelauncher.common.ExposedHelper.tx
import io.github.nayasis.simplelauncher.model.Link
import io.github.nayasis.simplelauncher.model.LinkTable
import io.github.nayasis.simplelauncher.model.repo
import io.github.nayasis.simplelauncher.model.vo.JsonLink
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.jdbc.deleteAll
import tornadofx.FileChooserMode
import tornadofx.SortedFilteredList
import tornadofx.asObservable
import tornadofx.runLater
import java.nio.file.Path
import java.util.*

private val logger = KotlinLogging.logger {}

@Inject
class LinkService {

    val links = SortedFilteredList(mutableListOf<Link>().asObservable())

    private val tokenIndex = TokenReferenceIndex()

    fun save(link: Link, refreshTable: Boolean = true) {
        val previousId = link.id
        link.syncTokenStorage()
        val isNew = link.id <= 0
        tx {
            LinkTable.repo.save(link)
            if (isNew) links.add(link)
        }
        tokenIndex.update(previousId, link)
        if (refreshTable) runLater { main.tableMain.refresh() }
    }

    fun importData(file: Path) {
        val list = file.readText().toObject<List<JsonLink>>().map { it.toLink() }
        logger.debug { "write links to DB (count: ${list.size})" }
        tx { list.forEach { LinkTable.repo.create(it) } }
    }

    fun countAll(): Int = tx(readOnly = true) { LinkTable.repo.select().count() }

    fun loadAll(worker: ((index: Int, link: Link) -> Unit)? = null) {
        val buffer = LinkedList<Link>()
        tx(readOnly = true) {
            LinkTable.repo.select().orderBy(LinkTable.title).forEachIndexed { i, link ->
                buffer.add(link)
                worker?.invoke(i + 1, link)
            }
        }
        links.run {
            clear()
            addAll(buffer)
        }
        tokenIndex.rebuild(buffer)
    }

    fun exportData(file: Path) {
        val jsonLinks = tx(readOnly = true) { LinkTable.repo.selectAll().map { JsonLink(it) } }
        file.writeText(Reflector.toJson(jsonLinks, pretty = true))
    }

    fun deleteAll() {
        tx {
            LinkTable.deleteAll()
            config.historyKeyword.clear()
            links.clear()
            tokenIndex.clear()
        }
    }

    fun delete(link: Link) {
        tx {
            LinkTable.repo.delete(link)
            config.historyKeyword.remove(link.title ?: "")
            links.remove(link)
            tokenIndex.remove(link)
        }
    }

    fun groupTokenSuggestions(excludedTokens: Iterable<String> = emptyList()): List<String> =
        tokenIndex.groupSuggestions(excludedTokens)

    fun hashtagSuggestions(excludedTokens: Iterable<String> = emptyList()): List<String> =
        tokenIndex.hashtagSuggestions(excludedTokens)

    fun openImportPicker(): Path? =
        filePicker("msg.file.import", "*.json", "msg.file.import.description")

    fun openExportPicker(): Path? =
        filePicker("msg.file.export", "*.json", "msg.file.import.description", mode = FileChooserMode.Save)

    fun openIconPicker(): Path? =
        filePicker("msg.file.icon", "*.*", "msg.file.icon.description")

    fun openExecutorPicker(): Path? =
        filePicker("msg.file.add", "*.*", "msg.file.add.description")

    fun openIconSavePicker(initialDirectory: Path, initialFileName: String): Path? =
        filePicker("msg.file.icon", "*.jpg", "msg.file.icon.save.description", initialDirectory, initialFileName, FileChooserMode.Save)

    private fun filePicker(
        title: String,
        extension: String,
        description: String,
        initialDirectory: Path? = null,
        initialFileName: String? = null,
        mode: FileChooserMode = FileChooserMode.Single,
    ): Path? = Dialog.filePicker(
        title            = title.message(),
        extension        = extension,
        description      = description.message(),
        initialDirectory = initialDirectory ?: config.filePickerInitialDirectory?.toPath(),
        initialFileName  = initialFileName,
        mode             = mode,
        owner            = main.primaryStage,
    ).firstOrNull().also { it?.let { config.filePickerInitialDirectory = it.directory.pathString } }

    fun openFolder(link: Link) {
        link.toPath()?.directory?.let {
            if (it.notExists()) Dialog.error("msg.error.no.directory".message().format(it))
            else Desktop.open(it.toFile())
        }
    }

    fun copyFolder(link: Link) {
        Desktop.clipboard.set((link.toPath()?.directory ?: link.path).toString())
    }

}

// ─── Token frequency index ───────────────────────────────────────────────────

private data class LinkTokenSnapshot(
    val id: Long,
    val group: Set<String>,
    val hashtag: Set<String>,
)

private class TokenReferenceIndex {

    private val snapshots = HashMap<Long, LinkTokenSnapshot>()
    private val groups    = HashMap<String, Int>()
    private val hashtags  = HashMap<String, Int>()

    fun rebuild(links: Iterable<Link>) {
        clear()
        links.forEach { it.toSnapshot()?.let(::add) }
    }

    fun update(previousId: Long, link: Link) {
        snapshots.remove(previousId)?.let { adjustCounts(it, -1) }
        link.toSnapshot()?.let(::add)
    }

    fun remove(link: Link) {
        snapshots.remove(link.id)?.let { adjustCounts(it, -1) }
    }

    fun clear() {
        snapshots.clear()
        groups.clear()
        hashtags.clear()
    }

    fun groupSuggestions(excludedTokens: Iterable<String> = emptyList()): List<String> =
        rank(groups, excludedTokens.normalized())

    fun hashtagSuggestions(excludedTokens: Iterable<String> = emptyList()): List<String> =
        rank(hashtags, excludedTokens.normalized())

    private fun add(snapshot: LinkTokenSnapshot) {
        snapshots[snapshot.id] = snapshot
        adjustCounts(snapshot, 1)
    }

    private fun adjustCounts(snapshot: LinkTokenSnapshot, delta: Int) {
        adjust(groups, snapshot.group, delta)
        adjust(hashtags, snapshot.hashtag, delta)
    }

    private fun adjust(counts: MutableMap<String, Int>, tokens: Set<String>, delta: Int) {
        tokens.forEach { token ->
            val next = (counts[token] ?: 0) + delta
            if (next > 0) counts[token] = next else counts.remove(token)
        }
    }

    private fun Link.toSnapshot(): LinkTokenSnapshot? {
        if (id <= 0) return null
        return LinkTokenSnapshot(id, group.normalized(), hashtag.normalized())
    }

    private fun rank(counts: Map<String, Int>, excluded: Set<String>): List<String> =
        counts.entries
            .filter { it.key !in excluded }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.key })
            .map { it.key }

    private fun Iterable<String>.normalized(): Set<String> =
        mapNotNull { it.trim().ifEmpty { null } }.toSet()

}
