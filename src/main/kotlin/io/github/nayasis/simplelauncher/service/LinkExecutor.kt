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
                            run(LinkCommand(link, file), wait=true)
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

        val nextCommands = toCommands(link.commandNext, link.workingDirectory)

        executeSequential(
            commands = toCommands(link.commandPrev, link.workingDirectory),
            showConsole = link.showConsole,
        )

        execute(
            command = link.toCommand(),
            wait = wait || nextCommands.isNotEmpty(),
            showConsole = link.showConsole,
            keepTerminalOpen = link.showConsole && !wait,
        )

        executeSequential(
            commands = nextCommands,
            showConsole = link.showConsole,
        )

    }

    private fun toCommands(script: String, workingDirectory: String?): List<Command> {
        return script
            .tokenize("\n\r")
            .map { Command(it, workingDirectory) }
            .filterNot { it.isEmpty() }
    }

    private fun executeSequential(commands: Collection<Command>, showConsole: Boolean) {
        commands.forEach { command ->
            execute(
                command = command,
                wait = true,
                showConsole = showConsole,
                keepTerminalOpen = false,
            )
        }
    }

    private fun execute(command: Command, wait: Boolean, showConsole: Boolean, keepTerminalOpen: Boolean) {
        if( command.isEmpty() ) return
        main.printCommand("$command")
        if(showConsole) {
            runInTerminal(command, keepTerminalOpen)
        } else {
            runInBackground(command, wait)
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
        Terminal(onFail = { e ->
            runAwait {
                Dialog.error(e)
            }
        }).run {
            show()
            runCatching { run(command) }
            if(!wait) close()
        }
    }

}

