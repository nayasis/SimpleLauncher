package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.path.*
import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.basica.core.string.tokenize
import com.github.nayasis.kotlin.javafx.misc.Desktop
import com.github.nayasis.kotlin.javafx.misc.set
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.common.Context.Companion.main
import com.github.nayasis.simplelauncher.jpa.entity.Link
import mu.KotlinLogging
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecuteResultHandler
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.ExecuteException
import org.springframework.stereotype.Service
import tornadofx.runLater
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime

private val logger = KotlinLogging.logger{}

@Service
class LinkExecutor(
    private val linkService: LinkService
) {

    fun run(link: Link, files: Collection<File>? = null) {

        linkService.save( link.apply {
            lastExecDate = LocalDateTime.now()
            executeCount++
        })
        main.tableMain.refresh()

        if( files == null ) {
            runLater { run(LinkCommand(link)) }
        } else {
            if( link.eachExecution ) {
                Dialog.progress {
                    files.forEachIndexed { index, file ->
                        val linkCmd = LinkCommand(link, file)
                        updateTitle(linkCmd.toCommand())
                        updateProgress(index.toLong(),files.size.toLong())
                        run(linkCmd,files.size > 1)
                    }
                }
            } else {
                runLater { run(LinkCommand(link,files),false) }
            }
        }

    }

    private fun run(linkCmd: LinkCommand, wait: Boolean = false) {

        linkCmd.commandPrev.tokenize("\n").forEach { run(it,linkCmd.workingDirectory,true) }

        val cmd = linkCmd.toCommand()

        main.printCommand(cmd)

        run(cmd, linkCmd.workingDirectory, wait || linkCmd.showConsole)

        linkCmd.commandNext.tokenize("\n").forEach { run(it,linkCmd.workingDirectory,true) }

    }

    private fun run(command: String?, workingDirectory: Path?, wait: Boolean): Boolean {

        if( command.isNullOrBlank() ) return false

        val executor = DefaultExecutor()
        val resultHandler = object:DefaultExecuteResultHandler() {
            override fun onProcessFailed(e: ExecuteException?) {
                runLater {
                    e?.cause?.let { Dialog.error(it) }
                }
            }
        }

        val cli = CommandLine.parse(command)

        if( workingDirectory?.exists() == true )
            executor.workingDirectory = workingDirectory.toFile().directory

        logger.debug { ">> command : $cli" }

        executor.execute(cli, resultHandler)

        if( wait )
            resultHandler.waitFor()

        return true

    }

    fun openFolder(link: Link) {
        LinkCommand(link).path?.directory?.let {
            if( it.notExists() ) {
                Dialog.error("msg.err.005".message().format(it) )
            } else {
                Desktop.open(it.toFile())
            }
        }
    }

    fun copyFolder(link: Link) {
        val path = LinkCommand(link).path?.directory ?: link.path
        Desktop.clipboard.set(path.toString())
    }

}