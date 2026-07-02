package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.kotlin.basica.core.string.tokenize
import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.kotlin.basica.exec.CommandExecutor
import io.github.nayasis.kotlin.javafx.app.di.Inject
import io.github.nayasis.kotlin.javafx.misc.runAwait
import io.github.nayasis.kotlin.javafx.stage.Dialog
import io.github.nayasis.kotlin.javafx.stage.progress.ProgressDialog
import io.github.nayasis.simplelauncher.common.Context.Companion.linkService
import io.github.nayasis.simplelauncher.common.Context.Companion.main
import io.github.nayasis.simplelauncher.model.Link
import io.github.nayasis.simplelauncher.view.Terminal
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.scene.control.Button
import javafx.scene.control.Tooltip
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

    private val childWindows = LauncherChildWindows()
    private val runningExecutors = LinkedHashSet<CommandExecutor>()
    private var runningTerminalCount = 0

    fun run(link: Link, files: Collection<File>? = null) {

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

    fun hideChildWindows() = childWindows.hide()

    fun restoreChildWindows() = childWindows.restore()

    fun hasRunningWork(): Boolean {
        if(childWindows.hasProgressDialogs()) {
            return true
        }
        synchronized(runningExecutors) {
            runningExecutors.removeIf { !it.isAlive }
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
        dialog = Dialog.progress(title, headerButton = context.queueButton).setOnDone {
            context.control.updateExecutor(null)
            unregisterProgressDialog(dialog)
        }
        registerProgressDialog(dialog, context.popOver)
        dialog.runAsync {
            task(it, context.control)
        }
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
        childWindows.register(dialog)
    }

    private fun registerProgressDialog(dialog: ProgressDialog, popOver: ProgressQueuePopOver) {
        childWindows.register(dialog, popOver)
    }

    private fun unregisterProgressDialog(dialog: ProgressDialog) {
        childWindows.unregister(dialog)
    }

    private fun refreshProgressQueue(dialog: ProgressDialog) {
        childWindows.refreshProgressQueue(dialog)
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
            if(!wait) {
                return
            }
            registerExecutor(executor)
            onExecutorChanged?.invoke(executor)
            try {
                executor.waitFor()
            } finally {
                onExecutorChanged?.invoke(null)
                unregisterExecutor(executor)
            }
        } catch (e: Exception) {
            throw RuntimeException("msg.error.runtime".message().format("$command")).apply { this.stackTrace = e.stackTrace }
        }
    }

    private fun runInTerminal(command: Command, wait: Boolean = false ) {
        if( command.isEmpty() ) return
        logger.debug { "- command: $command" }
        var runningRegistered = false
        fun unregisterRunningTerminalOnce() {
            synchronized(this) {
                if(runningRegistered) {
                    runningRegistered = false
                    unregisterRunningTerminal()
                }
            }
        }
        val terminal = Terminal(
            onAlways = {
                unregisterRunningTerminalOnce()
            },
            onFail = { e ->
            runAwait {
                Dialog.error(e)
            }
        })
        registerRunningTerminal()
        runningRegistered = true
        try {
            childWindows.register(terminal)
            terminal.run {
                childWindows.show(this)
                runCatching { run(command) }
                if(!wait) {
                    close()
                    childWindows.unregister(this)
                }
            }
        } finally {
            unregisterRunningTerminalOnce()
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
