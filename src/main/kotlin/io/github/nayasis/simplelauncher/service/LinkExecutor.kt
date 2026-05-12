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
import javafx.scene.control.Button
import javafx.scene.control.Tooltip
import javafx.stage.WindowEvent
import tornadofx.runLater
import java.io.File
import java.time.LocalDateTime

private val logger = KotlinLogging.logger{}

private data class ProgressQueueContext(
    val queueButton: Button,
    val control: ProgressQueueControl,
    val popOver: ProgressQueuePopOver,
)

@Inject
class LinkExecutor{

    private val progressDialogs = LinkedHashSet<ProgressDialog>()
    private val hiddenProgressDialogs = LinkedHashSet<ProgressDialog>()
    private val progressQueuePopOvers = LinkedHashMap<ProgressDialog, ProgressQueuePopOver>()
    private val terminals = LinkedHashSet<Terminal>()
    private val hiddenTerminals = LinkedHashSet<Terminal>()
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
                        val queue = ProgressFileQueue(files)
                        openProgressDialog(link.title, queue) { dialog, control ->
                            while (queue.hasNext()) {
                                val item = queue.next() ?: continue
                                updateProgress(dialog, queue, item)
                                run(LinkCommand(link, item.file), wait=true, onExecutorChanged=control::updateExecutor)
                                queue.finish(item)
                                control.updateExecutor(null)
                            }
                        }
                    } else {
                        val queue = ProgressFileQueue(files)
                        val progress = openProgressDialog(link.title, queue)
                        while (queue.hasNext()) {
                            val item = queue.next() ?: continue
                            updateProgress(progress, queue, item)
                            run(LinkCommand(link, item.file), wait=true)
                            queue.finish(item)
                            refreshProgressQueue(progress)
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

    fun hideChildWindows() = runLater {
        hideProgressDialogs()
        hideTerminals()
    }

    fun restoreChildWindows() = runLater {
        restoreProgressDialogs()
        restoreTerminals()
    }

    private fun hideProgressDialogs() {
        synchronized(progressDialogs) {
            progressDialogs.forEach { dialog ->
                if(dialog.stage.isShowing) {
                    hiddenProgressDialogs.add(dialog)
                    hideProgressQueue(dialog)
                    dialog.stage.hide()
                }
            }
        }
    }

    private fun restoreProgressDialogs() {
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

    private fun hideTerminals() {
        synchronized(terminals) {
            terminals.forEach { terminal ->
                if(terminal.isShowing) {
                    hiddenTerminals.add(terminal)
                    terminal.hide()
                }
            }
        }
    }

    private fun restoreTerminals() {
        synchronized(terminals) {
            hiddenTerminals.toList().forEach { terminal ->
                if(terminal !in terminals) {
                    hiddenTerminals.remove(terminal)
                    return@forEach
                }
                runCatching {
                    if(!terminal.isShowing) {
                        terminal.show()
                    }
                }.onFailure {
                    unregisterTerminalWindow(terminal)
                }
                hiddenTerminals.remove(terminal)
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

    private fun openProgressDialog(title: String?, queue: ProgressFileQueue): ProgressDialog {
        lateinit var dialog: ProgressDialog
        val context = createProgressQueueContext(queue) {
            updateProgress(dialog, queue, queue.currentItem())
            refreshProgressQueue(dialog)
        }
        dialog = Dialog.progress(title, headerButton = context.queueButton).also {
            registerProgressDialog(it, context.popOver)
        }
        return dialog
    }

    private fun openProgressDialog(
        title: String?,
        queue: ProgressFileQueue,
        task: (dialog: ProgressDialog, control: ProgressQueueControl) -> Unit,
    ): ProgressDialog {
        lateinit var dialog: ProgressDialog
        val context = createProgressQueueContext(queue) {
            updateProgress(dialog, queue, queue.currentItem())
            refreshProgressQueue(dialog)
        }
        dialog = Dialog.progress(title, headerButton = context.queueButton) {
            task(it, context.control)
        }.setOnDone {
            context.control.updateExecutor(null)
            unregisterProgressDialog(dialog)
        }
        registerProgressDialog(dialog, context.popOver)
        return dialog
    }

    private fun createProgressQueueContext(queue: ProgressFileQueue, onPendingRemoved: () -> Unit): ProgressQueueContext {
        lateinit var popOver: ProgressQueuePopOver
        val control = ProgressQueueControl(queue) {
            popOver.refreshIfShowing()
        }
        val queueButton = Button("☰").apply {
            tooltip = Tooltip("label.work.queue.tooltip".message())
            style = "-fx-padding: 0 4 0 4; -fx-min-width: 20px; -fx-pref-width: 20px; -fx-max-width: 20px; -fx-min-height: 18px; -fx-pref-height: 18px; -fx-max-height: 18px; -fx-font-size: 10px;"
            setOnAction { event ->
                event.consume()
                popOver.toggle(this)
            }
        }
        popOver = ProgressQueuePopOver(
            items = queue,
            control = control,
            onPendingRemoved = onPendingRemoved,
        )
        return ProgressQueueContext(queueButton, control, popOver)
    }

    private fun updateProgress(dialog: ProgressDialog, queue: ProgressFileQueue, item: ProgressFileQueueItem?) {
        dialog.updateMessage(item?.title)
        dialog.updateProgress(queue.currNo, queue.size.coerceAtLeast(1))
        dialog.updateSubMessage(
            if (queue.size > 1) {
                "(${queue.currNo}/${queue.size})"
            } else {
                ""
            }
        )
    }

    private fun registerProgressDialog(dialog: ProgressDialog) {
        synchronized(progressDialogs) {
            progressDialogs.add(dialog)
        }
    }

    private fun registerProgressDialog(dialog: ProgressDialog, popOver: ProgressQueuePopOver) {
        synchronized(progressDialogs) {
            progressDialogs.add(dialog)
            progressQueuePopOvers[dialog] = popOver
        }
    }

    private fun unregisterProgressDialog(dialog: ProgressDialog) {
        synchronized(progressDialogs) {
            progressDialogs.remove(dialog)
            hiddenProgressDialogs.remove(dialog)
            progressQueuePopOvers.remove(dialog)?.hide()
        }
    }

    private fun hideProgressQueue(dialog: ProgressDialog) {
        progressQueuePopOvers[dialog]?.hide()
    }

    private fun refreshProgressQueue(dialog: ProgressDialog) {
        progressQueuePopOvers[dialog]?.refreshIfShowing()
    }

    private fun run(link: LinkCommand, wait: Boolean = false, onExecutorChanged: ((CommandExecutor?) -> Unit)? = null) {

        val nextCommands = toCommands(link.commandNext, link.workingDirectory)

        executeSequential(
            commands = toCommands(link.commandPrev, link.workingDirectory),
            showConsole = link.showConsole,
            onExecutorChanged = onExecutorChanged,
        )

        execute(
            command = link.toCommand(),
            wait = wait || nextCommands.isNotEmpty(),
            showConsole = link.showConsole,
            keepTerminalOpen = link.showConsole && !wait,
            onExecutorChanged = onExecutorChanged,
        )

        executeSequential(
            commands = nextCommands,
            showConsole = link.showConsole,
            onExecutorChanged = onExecutorChanged,
        )

    }

    private fun toCommands(script: String, workingDirectory: String?): List<Command> {
        return script
            .tokenize("\n\r")
            .map { Command(it, workingDirectory) }
            .filterNot { it.isEmpty() }
    }

    private fun executeSequential(commands: Collection<Command>, showConsole: Boolean, onExecutorChanged: ((CommandExecutor?) -> Unit)? = null) {
        commands.forEach { command ->
            execute(
                command = command,
                wait = true,
                showConsole = showConsole,
                keepTerminalOpen = false,
                onExecutorChanged = onExecutorChanged,
            )
        }
    }

    private fun execute(command: Command, wait: Boolean, showConsole: Boolean, keepTerminalOpen: Boolean, onExecutorChanged: ((CommandExecutor?) -> Unit)? = null) {
        if( command.isEmpty() ) return
        main.printCommand("$command")
        if(showConsole) {
            runInTerminal(command, keepTerminalOpen)
        } else {
            runInBackground(command, wait, onExecutorChanged)
        }
    }

    private fun runInBackground(command: Command, wait: Boolean, onExecutorChanged: ((CommandExecutor?) -> Unit)? = null) {
        if( command.isEmpty() ) return
        logger.debug { "- command: $command" }
        try {
            val executor = command.run()
            registerExecutor(executor)
            onExecutorChanged?.invoke(executor)
            if(wait) {
                try {
                    executor.waitFor()
                } finally {
                    onExecutorChanged?.invoke(null)
                    unregisterExecutor(executor)
                }
            } else {
                Thread {
                    try {
                        executor.waitFor()
                    } finally {
                        onExecutorChanged?.invoke(null)
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
        registerRunningTerminal()
        val terminal = Terminal(
            onAlways = {
                unregisterRunningTerminal()
            },
            onFail = { e ->
            runAwait {
                Dialog.error(e)
            }
        })
        registerTerminalWindow(terminal)
        terminal.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST) {
            unregisterTerminalWindow(terminal)
        }
        terminal.run {
            show()
            runCatching { run(command) }
            if(!wait) {
                close()
                unregisterTerminalWindow(this)
            }
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

    private fun registerTerminalWindow(terminal: Terminal) {
        synchronized(terminals) {
            terminals.add(terminal)
        }
    }

    private fun unregisterTerminalWindow(terminal: Terminal) {
        synchronized(terminals) {
            terminals.remove(terminal)
            hiddenTerminals.remove(terminal)
        }
    }

    private fun registerRunningTerminal() {
        synchronized(this) {
            runningTerminalCount++
        }
    }

    private fun unregisterRunningTerminal() {
        synchronized(this) {
            if(runningTerminalCount > 0) {
                runningTerminalCount--
            }
        }
    }

}
