package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.extention.ifEmpty
import com.github.nayasis.kotlin.basica.core.path.*
import com.github.nayasis.kotlin.basica.core.string.format.DEFAULT_BINDER
import com.github.nayasis.kotlin.basica.core.string.format.ExtractPattern
import com.github.nayasis.kotlin.basica.core.string.format.Formatter
import com.github.nayasis.kotlin.basica.core.string.toPath
import com.github.nayasis.simplelauncher.jpa.entity.Link
import com.github.nayasis.simplelauncher.jpa.repository.LinkRepository
import mu.KotlinLogging
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecuteResultHandler
import org.apache.commons.exec.DefaultExecutor
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path


private val logger = KotlinLogging.logger{}

private val PATTERN_KEYWORD = ExtractPattern("#\\{([^\\s{}]*?)}".toPattern())

@Service
class LinkExecutor(
    private val linkService: LinkService,
    private val linkRepository: LinkRepository
) {

    fun run(link: Link) {



    }


    fun run(command: String?, wait: Boolean): Boolean {

        if( command.isNullOrBlank() ) return false

        val executor = DefaultExecutor()
        val resultHandler = DefaultExecuteResultHandler()

        val cli = CommandLine.parse(command)

        command.toPath().let {
            if(it.exists()) executor.workingDirectory = it.directory.toFile() }

        executor.execute(cli, resultHandler)

        if( wait )
            resultHandler.waitFor()

        return true

    }


    private fun getExecutionPath(link: Link): Path? {

        var path = link.path?.toPath().also { if(it==null) return null }!!
        if( path.exists() ) return path

        path = rootPath() / link.path.ifEmpty{""}
        if( path.exists() ) return path

        path = rootPath() / link.relativePath.ifEmpty{""}
        if( path.exists() ) {
            link.path = path.pathString
            linkService.update(link)
            return path
        }

        return null

    }

    private fun wrapDoubleQuote(value: String) = "\"${value.replace("\"", "\\\"")}\""

    private fun getParameter(file: File?): Map<String,String> {
        return HashMap<String,String>().apply {
            if( file == null || ! file.exists() ) return this
            this["filepath"] = file.absolutePath
            this["dir"]      = if (file.isDirectory) file.path else file.parent
            this["filename"] = file.name
            this["name"]     = file.nameWithoutExtension
            this["ext"]      = file.extension
            this["home"]     = userHome().pathString
        }
    }

    private fun bindOption(option: String, param: Map<String,String>): String {
        return Formatter().bind( PATTERN_KEYWORD, option, DEFAULT_BINDER, false, param )
    }


}