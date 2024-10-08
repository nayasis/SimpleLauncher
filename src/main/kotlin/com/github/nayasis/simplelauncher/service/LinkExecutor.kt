package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.basica.core.string.tokenize
import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.kotlin.javafx.misc.runSync
import com.github.nayasis.kotlin.javafx.property.InsetProperty
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.common.Context.Companion.config
import com.github.nayasis.simplelauncher.common.Context.Companion.linkService
import com.github.nayasis.simplelauncher.common.Context.Companion.main
import com.github.nayasis.simplelauncher.model.Link
import com.github.nayasis.simplelauncher.view.Terminal
import javafx.stage.Stage
import mu.KotlinLogging
import tornadofx.runLater
import java.io.File
import java.time.LocalDateTime

private val logger = KotlinLogging.logger{}

class LinkExecutor{

    fun run(link: Link, files: Collection<File>? = null) {

        link.title?.let { config.historyKeyword.add(it) }

        linkService.save( link.apply { executedAt = LocalDateTime.now() })
        runLater { main.tableMain.refresh() }

        runLater {
            if( files.isNullOrEmpty() ) {
                run(LinkCommand(link))
            } else if( files.size == 1 ) {
                run(LinkCommand(link,files.first()))
            } else {
                if( link.executeEach ) {
                    if( ! link.showConsole ) {
                        Dialog.progress(link.title) {
                            files.forEachIndexed { index, file ->
                                it.updateProgress(index + 1,files.size)
                                it.updateMessage(file.name)
                                run(LinkCommand(link, file),wait=true)
                            }
                        }
                    } else {
                        val progress = Dialog.progress(link.title)
                        files.forEachIndexed { index, file ->
                            progress.updateProgress(index + 1, files.size)
                            progress.updateMessage(file.name)
                            val cmd = LinkCommand(link, file)
                            logger.debug { ">> command : $cmd" }
                            Terminal(cmd.toCommand(),
                                onFail = { throwable ->
                                    runSync {
                                        Dialog.error(throwable)
                                    }
                                },
                                onDone = {
                                    runLater {
                                        it.close()
                                    }
                                }
                            ).showAndWait()
                        }
                        progress.close()
                    }
                } else {
                    run(LinkCommand(link,files),false)
                }
            }
            linkService.save( link.apply { executeCount++ })
            runLater { main.tableMain.refresh() }
        }

    }

    private fun run(linkCmd: LinkCommand, wait: Boolean = false) {
        with(linkCmd) {
            val command = toCommand()
            commandPrev.tokenize("\n").forEach { run(Command(it,workingDirectory),true) }
            main.printCommand("$command")
            run(command, wait || showConsole, showConsole)
            commandNext.tokenize("\n").forEach { run(Command(it,workingDirectory),true) }
        }
    }

    private fun run(command: Command, wait: Boolean, showConsole: Boolean = false, closeConsoleWhenDone: Boolean = false) {
        if( command.isEmpty() ) return
        logger.debug { ">> command : $command" }
        if( showConsole ) {
            val terminal = Terminal(command, onFail = { e ->
                throw RuntimeException("msg.error.runtime".message().format("$command")).apply { this.stackTrace = e.stackTrace }
            }, onDone = {
                if(closeConsoleWhenDone) {
                    runLater { it.close() }
                }
            })
            if(wait) {
                terminal.showAndWait()
            } else {
                terminal.show()
            }
        } else {
            try {
                command.run().also { if(wait) it.waitFor() }
            } catch (e: Exception) {
                throw RuntimeException("msg.error.runtime".message().format("$command")).apply { this.stackTrace = e.stackTrace }
            }
        }
    }

    private fun setStageToMiddle(stage: Stage, parentInset: InsetProperty?) {
        parentInset?.let {
            stage.x = it.x + it.width  / 2 - stage.width  / 2
            stage.y = it.y + it.height / 2 - stage.height / 2
        }
    }

}

