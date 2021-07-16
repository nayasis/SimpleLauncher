package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.jpa.repository.LinkRepository
import com.github.nayasis.simplelauncher.jpa.vo.JsonLink
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.File

private val logger = KotlinLogging.logger{}

private const val FILE_EXT_DESC = "Data File (*.sl)"
private const val FILE_EXT = "*.sl"

private const val INITIAL_DIRECTORY = "link.path.fileopen.initial"

@Component
class LinkService(
    private val linkRepository: LinkRepository,
    private val configService: ConfigService,
) {

    @Transactional
    fun importData(file: File) {

        val json = file.readText()

        val links = Reflector.toObject<List<JsonLink>>(json)

        linkRepository.saveAll(links.map { it.toLink() })

    }

    private fun getInitialDirectory(): File {

        if( configService[INITIAL_DIRECTORY].isNullOrEmpty() ) {

        }

    }



}