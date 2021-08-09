package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.path.directory
import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.common.Context
import com.github.nayasis.simplelauncher.jpa.entity.Link
import com.github.nayasis.simplelauncher.jpa.repository.LinkRepository
import com.github.nayasis.simplelauncher.jpa.vo.JsonLink
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.File

private val logger = KotlinLogging.logger{}

private const val FILE_EXT = "*.sl"

@Component
class LinkService(
    private val linkRepository: LinkRepository,
    private val configService: ConfigService,
) {

    @Transactional
    fun save(link: Link) {
        linkRepository.save(link)
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
        linkRepository.deleteAll()
    }

    @Transactional
    fun delete(link: Link) {
        linkRepository.delete(link)
        Context.main.links.remove(link)
    }

    fun openImportFilePicker(): File? {
        return Dialog.filePicker("msg.info.004".message(), FILE_EXT, "msg.info.011".message(), configService.filePickerInitialDirectory)
            .showOpenDialog(Context.main.primaryStage)
            .also {
                if( it != null )
                    configService.filePickerInitialDirectory = it.directory.path
            }
    }

    fun openExportFilePicker(): File? {
        return Dialog.filePicker("msg.info.003".message(), FILE_EXT, "msg.info.011".message(), configService.filePickerInitialDirectory)
            .showOpenDialog(Context.main.primaryStage)
            .also {
                if( it != null )
                    configService.filePickerInitialDirectory = it.directory.path
            }
    }

    fun openIconFilePicker(): File? {
        return Dialog.filePicker("msg.info.002".message(), "*.*", "msg.info.012".message(), configService.filePickerInitialDirectory)
            .showOpenDialog(Context.main.primaryStage)
            .also {
                if( it != null )
                    configService.filePickerInitialDirectory = it.directory.path
            }
    }

    fun openPathFilePicker(): File? {
        return Dialog.filePicker("msg.info.001".message(), "*.*", "msg.info.006".message(), configService.filePickerInitialDirectory)
            .showOpenDialog(Context.main.primaryStage)
            .also {
                if( it != null )
                    configService.filePickerInitialDirectory = it.directory.path
            }
    }

}