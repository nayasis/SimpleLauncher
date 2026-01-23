package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.kotlin.basica.core.string.tokenize
import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.kotlin.javafx.app.di.Inject
import io.github.nayasis.kotlin.javafx.misc.runAwait
import io.github.nayasis.kotlin.javafx.stage.Dialog
import io.github.nayasis.simplelauncher.common.Context.Companion.config
import io.github.nayasis.simplelauncher.common.Context.Companion.linkService
import io.github.nayasis.simplelauncher.common.Context.Companion.main
import io.github.nayasis.simplelauncher.model.Link
import io.github.nayasis.simplelauncher.view.Terminal
import io.github.oshai.kotlinlogging.KotlinLogging
import tornadofx.runLater
import java.io.File
import java.time.LocalDateTime

private val logger = KotlinLogging.logger{}

@Inject
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
                                run(LinkCommand(link, file), wait=true)
                            }
                        }
                    } else {
                        val progress = Dialog.progress(link.title)
                        files.forEachIndexed { index, file ->
                            progress.run {
                                updateProgress(index + 1, files.size)
                                updateMessage(file.name)
                            }
                            runInTerminal(LinkCommand(link, file).toCommand())
                        }
                        progress.close()
                    }
                } else {
                    run(LinkCommand(link,files), wait=false)
                }
            }
            linkService.save( link.apply { executeCount++ })
            runLater { main.tableMain.refresh() }
        }

    }

    private fun run(link: LinkCommand, wait: Boolean = false) {

        link.commandPrev.tokenize("\n\r").forEach {
            runInBackground(Command(it, link.workingDirectory),true)
        }

        val command = link.toCommand().also { main.printCommand("$it") }

        if(link.showConsole) {
            runInTerminal(command, true)
        } else {
            runInBackground(command, wait || link.commandNext.isNotEmpty())
        }

        link.commandNext.tokenize("\n\r").forEach {
            runInBackground(Command(it, link.workingDirectory),true)
        }

    }

    private fun runInBackground(command: Command, wait: Boolean) {
        if( command.isEmpty() ) return
        logger.debug { "- command: $command" }
        try {
            command.run().also { if(wait) it.waitFor() }
        } catch (e: Exception) {
            throw RuntimeException("msg.error.runtime".message().format("$command")).apply { this.stackTrace = e.stackTrace }
        }
    }

    private fun runInTerminal(command: Command, wait: Boolean = false ) {
        if( command.isEmpty() ) return
        logger.debug { "- command: $command" }
        Terminal(command, onFail = { e ->
            runAwait {
                Dialog.error(e)
            }
        }).run {
            runCatching { show() }
            if(!wait) close()
        }
    }

}

