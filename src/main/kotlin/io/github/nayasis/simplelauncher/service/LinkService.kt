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

    fun save(link: Link, refreshTable: Boolean = true) {
        link.syncHashtagStorage()
        val isNew = link.id <= 0
        tx {
            LinkTable.repo.save(link)
            if(isNew) {
                links.add(link)
            }
        }
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
        }
    }

    fun delete(link: Link) {
        tx {
            LinkTable.repo.delete(link)
            config.historyKeyword.remove(link.title ?: "")
            links.remove(link)
        }
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
