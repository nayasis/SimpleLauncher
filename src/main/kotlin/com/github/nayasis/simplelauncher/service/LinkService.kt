package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.io.directory
import com.github.nayasis.kotlin.basica.core.io.notExists
import com.github.nayasis.kotlin.basica.core.io.pathString
import com.github.nayasis.kotlin.basica.core.io.readText
import com.github.nayasis.kotlin.basica.core.io.writeText
import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.basica.core.string.toPath
import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.misc.Desktop
import com.github.nayasis.kotlin.javafx.misc.set
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.common.Context
import com.github.nayasis.simplelauncher.model.Link
import com.github.nayasis.simplelauncher.model.Links
import com.github.nayasis.simplelauncher.model.from
import com.github.nayasis.simplelauncher.model.save
import com.github.nayasis.simplelauncher.model.toLink
import com.github.nayasis.simplelauncher.model.vo.JsonLink
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.FileChooserMode
import tornadofx.SortedFilteredList
import tornadofx.asObservable
import tornadofx.runLater
import java.nio.file.Path
import java.util.*

private val logger = KotlinLogging.logger{}

class LinkService {

    val links = SortedFilteredList(mutableListOf<Link>().asObservable())

    fun save(link: Link, refreshTable: Boolean = true) {
        if(link.isNew)
            links.add(link)
        transaction {
            Links.save(link)
            commit()
        }
        if(refreshTable) {
            runLater {
                Context.main.tableMain.refresh()
            }
        }
    }

    fun importData(file: Path) {
        val jsonLinks = file.readText().let { Reflector.toObject<List<JsonLink>>(it) }.map { it.toLink() }
        transaction {
            jsonLinks.forEach { link ->
                Links.insert { it.from(link) }
            }
            commit()
        }
    }

    fun countAll(): Long {
        return transaction {
            Links.selectAll().count()
        }
    }

    fun loadAll(worker: ((index: Int, link: Link) -> Unit)? = null) {
        var i = 0
        val links = LinkedList<Link>()
        transaction {
            Links.selectAll().orderBy(Links.title, SortOrder.ASC).iterator().forEach { row ->
                val link = row.toLink()
                links.add(link)
                worker?.invoke(++i, link)
            }
        }
        this.links.run {
            clear()
            addAll(links)
        }
    }

    fun exportData(file: Path) {
        val dbLinks = transaction {
            Links.selectAll().map { it.toLink() }.map { JsonLink(it) }
        }
        logger.debug { dbLinks }
        file.writeText( Reflector.toJson(dbLinks, pretty = true))
    }

    fun deleteAll() {
        Context.config.historyKeyword.clear()
        transaction {
            Links.deleteAll()
            links.clear()
            commit()
        }
    }

    fun delete(link: Link) {
        Context.config.historyKeyword.remove(link.title ?: "")
        transaction {
            Links.deleteWhere { Links.id eq link.id }
            links.remove(link)
            commit()
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

    private fun filePicker(title: String, extension: String, description: String, mode: FileChooserMode = FileChooserMode.Single): Path? {
        return Dialog.filePicker(
            title = title.message(),
            extension = extension,
            description = description.message(),
            initialDirectory = Context.config.filePickerInitialDirectory?.toPath(),
            mode = mode,
            owner = Context.main.primaryStage
        ).firstOrNull().also {
            if( it != null )
                Context.config.filePickerInitialDirectory = it.directory.pathString
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