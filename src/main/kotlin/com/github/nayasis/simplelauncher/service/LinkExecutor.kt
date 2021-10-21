package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.path.*
import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.basica.core.string.tokenize
import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.kotlin.basica.exec.CommandExecutor
import com.github.nayasis.kotlin.javafx.misc.Desktop
import com.github.nayasis.kotlin.javafx.misc.set
import com.github.nayasis.kotlin.javafx.property.StageProperty
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.common.Context.Companion.main
import com.github.nayasis.simplelauncher.jpa.entity.Link
import com.github.nayasis.simplelauncher.view.terminal.Terminal
import javafx.stage.Modality
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
                    Dialog.progress(link.title) {
                        files.forEachIndexed { index, file ->
                            val cmd = LinkCommand(link, file)
                            updateMessage(file.name)
                            updateProgress(index + 1L,files.size.toLong())
                            run(cmd,wait=true)
                        }
                    }
                } else {
                    val progress = Dialog.progress(link.title).apply { initModality(Modality.NONE) }
                    files.forEachIndexed { index, file ->
                        progress.updateProgress(index + 1, files.size)
                        progress.updateMessage(file.name)
                        val cmd = LinkCommand(link, file)
                        Terminal(cmd.toCommand(),
                            onSuccess = { runLater { it.close() } },
                            onFail = { throwable, it ->
                                runLater {
                                    Dialog.error(throwable)
                                    it.close()
                                }
                            },
                            onDone = {
                                ConfigService.stageTerminal = StageProperty(it)
                            }
                        ).showAndWait()
                    }
                    progress.close()
                }
            } else {
                runLater { run(LinkCommand(link,files),false) }
            }
        }

    }

    private fun run(linkCmd: LinkCommand, wait: Boolean = false) {
        with(linkCmd) {
            commandPrev.tokenize("\n").forEach { run(Command(it,workingDirectory),true,showConsole) }
            main.printCommand("${toCommand()}")
            run(toCommand(), wait || showConsole, showConsole)
            commandNext.tokenize("\n").forEach { run(Command(it,workingDirectory),true,showConsole) }
        }
    }

    private fun run(command: Command, wait: Boolean, showConsole: Boolean = false) {
        if( command.isEmpty() ) return
//        logger.debug { ">> command : $command" }
        if( showConsole ) {
            val terminal = Terminal(command, onDone = { it.close() })
            if(wait) {
                terminal.showAndWait()
            } else {
                terminal.show()
            }
        } else {
            CommandExecutor().run(command).also { if(wait) it.waitFor() }
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