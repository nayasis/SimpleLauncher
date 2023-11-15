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
import com.github.nayasis.kotlin.javafx.preloader.NPreloader
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.common.Context
import com.github.nayasis.simplelauncher.main
import com.github.nayasis.simplelauncher.model.Link
import com.github.nayasis.simplelauncher.model.Links
import com.github.nayasis.simplelauncher.model.from
import com.github.nayasis.simplelauncher.model.save
import com.github.nayasis.simplelauncher.model.toLink
import com.github.nayasis.simplelauncher.model.vo.JsonLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.FileChooserMode
import tornadofx.SortedFilteredList
import tornadofx.asObservable
import tornadofx.runLater
import java.nio.file.Path
import java.util.LinkedList

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

    fun loadAll() {
        links.clear()
        transaction {
            Links.selectAll().orderBy(Links.title, SortOrder.ASC).map { it.toLink() }
        }.let {
            links.addAll(it)
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
        filePicker("msg.info.004","*.sl","msg.info.011")

    fun openExportPicker(): Path? =
        filePicker("msg.info.003","*.sl","msg.info.011", FileChooserMode.Save)

    fun openIconPicker(): Path? =
        filePicker("msg.info.002","*.*","msg.info.012")

    fun openExecutorPicker(): Path? =
        filePicker("msg.info.001","*.*","msg.info.006")

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
                Dialog.error("msg.err.005".message().format(it) )
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