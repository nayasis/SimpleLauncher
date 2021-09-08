package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.path.*
import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.basica.core.string.tokenize
import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.kotlin.basica.exec.CommandExecutor
import com.github.nayasis.kotlin.javafx.misc.Desktop
import com.github.nayasis.kotlin.javafx.misc.set
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.common.Context.Companion.main
import com.github.nayasis.simplelauncher.jpa.entity.Link
import com.github.nayasis.simplelauncher.view.terminal.Terminal
import mu.KotlinLogging
import org.springframework.stereotype.Service
import tornadofx.runLater
import java.io.File
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
        } else if( files.size == 1 ) {
            runLater { run(LinkCommand(link,files.first())) }
        } else {
            if( link.eachExecution ) {
                if( ! link.showConsole ) {
                    Dialog.progress {
                        files.forEachIndexed { index, file ->
                            val cmd = LinkCommand(link, file)
                            updateTitle(cmd.toCommand())
                            updateProgress(index.toLong(),files.size.toLong())
                            run(cmd,wait=true)
                        }
                    }
                } else {
                    Dialog.progress {
                        files.forEachIndexed { index, file ->
                            val cmd = LinkCommand(link, file)
                            updateTitle(cmd.toCommand())
                            updateProgress(index.toLong(),files.size.toLong())
                            Terminal("$cmd").showAndWait()
                        }
                    }
                }
            } else {
                runLater { run(LinkCommand(link,files),false) }
            }
        }

    }

    private fun run(linkCmd: LinkCommand, wait: Boolean = false, onTerminal: Boolean = false) {
        with(linkCmd) {
            commandPrev.tokenize("\n").forEach { run(it,workingDirectory,true,onTerminal) }
            main.printCommand(toCommand())
            run(toCommand(), workingDirectory, wait || showConsole)
            commandNext.tokenize("\n").forEach { run(it,workingDirectory,true,onTerminal) }
        }
    }

    private fun run(command: String?, workingDirectory: String?, wait: Boolean, onTerminal: Boolean = false) {
        if( command.isNullOrBlank() ) return
        val cli = Command(command,workingDirectory).also { logger.debug { ">> command : $it" } }
        if( onTerminal ) {
            val terminal = Terminal("$cli", onDone = { it.close() })
            if(wait) {
                terminal.showAndWait()
            } else {
                terminal.show()
            }
        } else {
            CommandExecutor().apply {
                onProcessFailed = { e -> Dialog.error(e) }
            }.run(cli).also{ if(wait) it.waitFor()}
        }
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