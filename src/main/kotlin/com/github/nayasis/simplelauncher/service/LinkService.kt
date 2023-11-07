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
import com.github.nayasis.simplelauncher.model.vo.JsonLink
import com.github.nayasis.simplelauncher.model.Link
import com.github.nayasis.simplelauncher.model.Links
import mu.KotlinLogging
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tornadofx.FileChooserMode
import java.nio.file.Path

private val logger = KotlinLogging.logger{}

@Component
class LinkService {

    fun save(link: Link) {
        transaction {
            link.generateKeyword()
            commit()
        }
    }

    @Transactional
    fun importData(file: Path) {
        val jsonLinks = file.readText().let { Reflector.toObject<List<JsonLink>>(it) }
        transaction {
            jsonLinks.forEach { it.createNew() }
            commit()
        }
    }

    fun exportData(file: Path) {
        val jsonLinks = Link.all().map { JsonLink(it) }
        file.writeText( Reflector.toJson(jsonLinks, pretty = true))
    }

    fun deleteAll() {
        Context.config.historyKeyword.clear()
        transaction {
            Links.deleteAll()
            commit()
        }
    }

    @Transactional
    fun delete(link: Link) {
        Context.config.historyKeyword.remove(link.title ?: "")
        transaction {
            Link.findById(link.id)?.delete()
            Context.main.links.remove(link)
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