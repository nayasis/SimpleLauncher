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
    private val tokenReferences = TokenReferenceIndex()

    fun save(link: Link, refreshTable: Boolean = true) {
        val previousId = link.id
        link.syncTokenStorage()
        val isNew = link.id <= 0
        tx {
            LinkTable.repo.save(link)
            if(isNew) {
                links.add(link)
            }
        }
        tokenReferences.update(previousId, link)
        if(refreshTable) {
            runLater {
                main.tableMain.refresh()
            }
        }
    }

    fun importData(file: Path) {
        val links = file.readText().toObject<List<JsonLink>>().map { it.toLink() }
        logger.debug { "write links to DB (count: ${links.size})" }
        tx {
            links.forEachIndexed { i, link ->
                LinkTable.repo.create(link)
            }
        }
    }

    fun countAll(): Int {
        return tx(readOnly = true) {
            LinkTable.repo.select().count()
        }
    }

    fun loadAll(worker: ((index: Int, link: Link) -> Unit)? = null) {
        val buffer = LinkedList<Link>()
        tx(readOnly = true) {
            LinkTable.repo.select().orderBy(LinkTable.title).forEachIndexed { i, link ->
                buffer.add(link)
                worker?.invoke(i+1, link)
            }
        }
        links.run {
            clear()
            addAll(buffer)
        }
        tokenReferences.rebuild(buffer)
    }

    fun exportData(file: Path) {
        val jsonLinks = tx(readOnly = true) {
            LinkTable.repo.selectAll().map { JsonLink(it) }
        }
        file.writeText( Reflector.toJson(jsonLinks, pretty = true))
    }

    fun deleteAll() {
        tx {
            LinkTable.deleteAll()
            config.historyKeyword.clear()
            links.clear()
            tokenReferences.clear()
        }
    }

    fun delete(link: Link) {
        tx {
            LinkTable.repo.delete(link)
            config.historyKeyword.remove(link.title ?: "")
            links.remove(link)
            tokenReferences.remove(link)
        }
    }

    fun groupTokenSuggestions(excludedTokens: Iterable<String> = emptyList()): List<String> {
        return tokenReferences.groupSuggestions(excludedTokens)
    }

    fun groupTokenSuggestions(selectedTokens: Iterable<String>, excludedTokens: Iterable<String>): List<String> {
        return tokenReferences.groupSuggestions(selectedTokens, excludedTokens)
    }

    fun hashtagSuggestions(excludedTokens: Iterable<String> = emptyList()): List<String> {
        return tokenReferences.hashtagSuggestions(excludedTokens)
    }

    fun openImportPicker(): Path? =
        filePicker("msg.file.import","*.sl","msg.file.import.description")

    fun openExportPicker(): Path? =
        filePicker("msg.file.export","*.sl","msg.file.import.description", FileChooserMode.Save)

    fun openIconPicker(): Path? =
        filePicker("msg.file.icon","*.*","msg.file.icon.description")

    fun openExecutorPicker(): Path? =
        filePicker("msg.file.add","*.*","msg.file.add.description")

    private fun filePicker(
        title: String,
        extension: String,
        description: String,
        mode: FileChooserMode = FileChooserMode.Single
    ): Path? {
        return Dialog.filePicker(
            title = title.message(),
            extension = extension,
            description = description.message(),
            initialDirectory = config.filePickerInitialDirectory?.toPath(),
            mode = mode,
            owner = main.primaryStage
        ).firstOrNull().also {
            if( it != null )
                config.filePickerInitialDirectory = it.directory.pathString
        }
    }

    fun openFolder(link: Link) {
        link.toPath()?.directory?.let {
            if( it.notExists() ) {
                Dialog.error("msg.error.no.directory".message().format(it) )
            } else {
                Desktop.open(it.toFile())
            }
        }
    }

    fun copyFolder(link: Link) {
        val path = link.toPath()?.directory ?: link.path
        Desktop.clipboard.set(path.toString())
    }

}

private data class LinkTokenSnapshot(
    val id: Long,
    val group: Set<String>,
    val hashtag: Set<String>,
)

private class TokenReferenceIndex {

    private val snapshots = HashMap<Long, LinkTokenSnapshot>()
    private val groupCounts = HashMap<String, Int>()
    private val hashtagCounts = HashMap<String, Int>()
    private val groupContextCounts = HashMap<Set<String>, MutableMap<String, Int>>()

    fun rebuild(links: Iterable<Link>) {
        clear()
        links.forEach { link ->
            snapshot(link)?.let(::add)
        }
    }

    fun update(previousId: Long, link: Link) {
        if(previousId > 0) {
            snapshots.remove(previousId)?.let(::removeCounts)
        }
        snapshot(link)?.let(::add)
    }

    fun remove(link: Link) {
        snapshots.remove(link.id)?.let(::removeCounts)
    }

    fun clear() {
        snapshots.clear()
        groupCounts.clear()
        hashtagCounts.clear()
        groupContextCounts.clear()
    }

    fun groupSuggestions(excludedTokens: Iterable<String>): List<String> {
        return rank(groupCounts, excludedTokens.normalizedTokenSet())
    }

    fun groupSuggestions(selectedTokens: Iterable<String>, excludedTokens: Iterable<String>): List<String> {
        val selected = selectedTokens.normalizedTokenSet()
        if(selected.isEmpty()) {
            return groupSuggestions(excludedTokens)
        }
        return rank(groupContextCounts[selected] ?: emptyMap(), excludedTokens.normalizedTokenSet() + selected)
    }

    fun hashtagSuggestions(excludedTokens: Iterable<String>): List<String> {
        return rank(hashtagCounts, excludedTokens.normalizedTokenSet())
    }

    private fun add(snapshot: LinkTokenSnapshot) {
        snapshots[snapshot.id] = snapshot
        addCounts(snapshot)
    }

    private fun addCounts(snapshot: LinkTokenSnapshot) {
        adjust(groupCounts, snapshot.group, 1)
        adjust(hashtagCounts, snapshot.hashtag, 1)
        adjustGroupContexts(snapshot.group, 1)
    }

    private fun removeCounts(snapshot: LinkTokenSnapshot) {
        adjust(groupCounts, snapshot.group, -1)
        adjust(hashtagCounts, snapshot.hashtag, -1)
        adjustGroupContexts(snapshot.group, -1)
    }

    private fun adjustGroupContexts(group: Set<String>, delta: Int) {
        if(group.size < 2) return
        properSubsets(group.toList()).forEach { selected ->
            val candidates = group - selected
            val counts = groupContextCounts.getOrPut(selected) { HashMap() }
            adjust(counts, candidates, delta)
            if(counts.isEmpty()) {
                groupContextCounts.remove(selected)
            }
        }
    }

    private fun properSubsets(tokens: List<String>): List<Set<String>> {
        val subsets = ArrayList<Set<String>>()
        fun collect(index: Int, selected: LinkedHashSet<String>) {
            if(index == tokens.size) {
                if(selected.isNotEmpty() && selected.size < tokens.size) {
                    subsets += HashSet(selected)
                }
                return
            }
            collect(index + 1, selected)
            selected += tokens[index]
            collect(index + 1, selected)
            selected -= tokens[index]
        }
        collect(0, LinkedHashSet())
        return subsets
    }

    private fun adjust(counts: MutableMap<String, Int>, tokens: Iterable<String>, delta: Int) {
        tokens.forEach { token ->
            val next = (counts[token] ?: 0) + delta
            if(next > 0) {
                counts[token] = next
            } else {
                counts.remove(token)
            }
        }
    }

    private fun snapshot(link: Link): LinkTokenSnapshot? {
        if(link.id <= 0) return null
        return LinkTokenSnapshot(
            id = link.id,
            group = link.group.normalizedTokenSet(),
            hashtag = link.hashtag.normalizedTokenSet(),
        )
    }

    private fun rank(counts: Map<String, Int>, excluded: Set<String>): List<String> {
        return counts.entries
            .asSequence()
            .filter { it.key !in excluded }
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.key }
            )
            .map { it.key }
            .toList()
    }

    private fun Iterable<String>.normalizedTokenSet(): Set<String> {
        return map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

}
