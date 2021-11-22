package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.path.directory
import com.github.nayasis.kotlin.basica.core.path.notExists
import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.basica.core.string.toFile
import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.misc.Desktop
import com.github.nayasis.kotlin.javafx.misc.set
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.common.Context
import com.github.nayasis.simplelauncher.jpa.entity.Link
import com.github.nayasis.simplelauncher.jpa.repository.LinkRepository
import com.github.nayasis.simplelauncher.jpa.vo.JsonLink
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tornadofx.*
import java.io.File

private val logger = KotlinLogging.logger{}

@Component
class LinkService(
    private val linkRepository: LinkRepository,
    private val linkExecutor: LinkExecutor,
) {

    @Transactional
    fun save(link: Link) {
        linkRepository.save(link.generateKeyword())
    }

    @Transactional
    fun importData(file: File) {
        val links = file.readText().let { Reflector.toObject<List<JsonLink>>(it) }.map { it.toLink() }
        linkRepository.saveAll(links)
    }

    fun exportData(file: File) {
        val jsonLinks = linkRepository.findAllByOrderByTitle().map { JsonLink(it) }
        file.writeText( Reflector.toJson(jsonLinks, pretty = true))
    }

    @Transactional
    fun deleteAll() {
        linkExecutor.history.clear()
        linkRepository.deleteAll()
    }

    @Transactional
    fun delete(link: Link) {
        linkExecutor.history.remove(link.title ?: "")
        linkRepository.delete(link)
        Context.main.links.remove(link)
    }

    fun openImportPicker(): File? =
        filePicker("msg.info.004","*.sl","msg.info.011")

    fun openExportPicker(): File? =
        filePicker("msg.info.003","*.sl","msg.info.011", FileChooserMode.Save)

    fun openIconPicker(): File? =
        filePicker("msg.info.002","*.*","msg.info.012")

    fun openExecutorPicker(): File? =
        filePicker("msg.info.001","*.*","msg.info.006")

    private fun filePicker(title: String, extension: String, description: String, mode: FileChooserMode = FileChooserMode.Single): File? {
        return Dialog.filePicker(
            title = title.message(),
            extension = extension,
            description = description,
            initialDirectory = ConfigService.filePickerInitialDirectory?.toFile(),
            mode = mode,
            owner = Context.main.primaryStage
        ).firstOrNull().also {
            if( it != null )
                ConfigService.filePickerInitialDirectory = it.directory.path
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