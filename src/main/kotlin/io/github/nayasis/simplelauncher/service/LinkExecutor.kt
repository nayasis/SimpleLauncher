package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.kotlin.basica.core.string.tokenize
import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.kotlin.basica.exec.CommandExecutor
import io.github.nayasis.kotlin.javafx.app.di.Inject
import io.github.nayasis.kotlin.javafx.misc.runAwait
import io.github.nayasis.kotlin.javafx.stage.Dialog
import io.github.nayasis.kotlin.javafx.stage.progress.ProgressDialog
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

    private val progressDialogs = LinkedHashSet<ProgressDialog>()
    private val hiddenProgressDialogs = LinkedHashSet<ProgressDialog>()
    private val runningExecutors = LinkedHashSet<CommandExecutor>()
    private var runningTerminalCount = 0

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
                        openProgressDialog(link.title) {
                            files.forEachIndexed { index, file ->
                                it.updateProgress(index + 1,files.size)
                                it.updateMessage(file.name)
                                run(LinkCommand(link, file), wait=true)
                            }
                        }
                    } else {
                        val progress = openProgressDialog(link.title)
                        files.forEachIndexed { index, file ->
                            progress.run {
                                updateProgress(index + 1, files.size)
                                updateMessage(file.name)
                            }
                            run(LinkCommand(link, file), wait=true)
                        }
                        progress.close()
                        unregisterProgressDialog(progress)
                    }
                } else {
                    run(LinkCommand(link,files), wait=false)
                }
            }
            linkService.save( link.apply { executeCount++ })
            runLater { main.tableMain.refresh() }
        }

    }

    fun hideProgressDialogs() = runLater {
        synchronized(progressDialogs) {
            progressDialogs.forEach { dialog ->
                if(dialog.stage.isShowing) {
                    hiddenProgressDialogs.add(dialog)
                    dialog.stage.hide()
                }
            }
        }
    }

    fun restoreProgressDialogs() = runLater {
        synchronized(progressDialogs) {
            hiddenProgressDialogs.toList().forEach { dialog ->
                if(dialog !in progressDialogs) {
                    hiddenProgressDialogs.remove(dialog)
                    return@forEach
                }
                runCatching {
                    if(!dialog.stage.isShowing) {
                        dialog.show()
                    }
                }.onFailure {
                    unregisterProgressDialog(dialog)
                }
                hiddenProgressDialogs.remove(dialog)
            }
        }
    }

    fun hasRunningWork(): Boolean {
        synchronized(progressDialogs) {
            if(progressDialogs.isNotEmpty()) {
                return true
            }
        }
        synchronized(runningExecutors) {
            if(runningExecutors.isNotEmpty()) {
                return true
            }
        }
        return synchronized(this) {
            runningTerminalCount > 0
        }
    }

    private fun openProgressDialog(title: String?): ProgressDialog {
        return Dialog.progress(title).also {
            registerProgressDialog(it)
        }
    }

    private fun openProgressDialog(title: String?, task: (dialog: ProgressDialog) -> Unit): ProgressDialog {
        lateinit var dialog: ProgressDialog
        dialog = Dialog.progress(title, task = task).setOnDone {
            unregisterProgressDialog(dialog)
        }
        registerProgressDialog(dialog)
        return dialog
    }

    private fun registerProgressDialog(dialog: ProgressDialog) {
        synchronized(progressDialogs) {
            progressDialogs.add(dialog)
        }
    }

    private fun unregisterProgressDialog(dialog: ProgressDialog) {
        synchronized(progressDialogs) {
            progressDialogs.remove(dialog)
            hiddenProgressDialogs.remove(dialog)
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
            val executor = command.run()
            registerExecutor(executor)
            if(wait) {
                try {
                    executor.waitFor()
                } finally {
                    unregisterExecutor(executor)
                }
            } else {
                Thread {
                    try {
                        executor.waitFor()
                    } finally {
                        unregisterExecutor(executor)
                    }
                }.apply {
                    isDaemon = true
                    start()
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("msg.error.runtime".message().format("$command")).apply { this.stackTrace = e.stackTrace }
        }
    }

    private fun runInTerminal(command: Command, wait: Boolean = false ) {
        if( command.isEmpty() ) return
        logger.debug { "- command: $command" }
        registerTerminal()
        Terminal(
            onAlways = {
                unregisterTerminal()
            },
            onFail = { e ->
            runAwait {
                Dialog.error(e)
            }
        }).run {
            show()
            runCatching { run(command) }
            if(!wait) close()
        }
    }

    private fun registerExecutor(executor: CommandExecutor) {
        synchronized(runningExecutors) {
            runningExecutors.add(executor)
        }
    }

    private fun unregisterExecutor(executor: CommandExecutor) {
        synchronized(runningExecutors) {
            runningExecutors.remove(executor)
        }
    }

    private fun registerTerminal() {
        synchronized(this) {
            runningTerminalCount++
        }
    }

    private fun unregisterTerminal() {
        synchronized(this) {
            if(runningTerminalCount > 0) {
                runningTerminalCount--
            }
        }
    }

}

