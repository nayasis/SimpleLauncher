package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.kotlin.basica.core.string.tokenize
import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.kotlin.basica.exec.CommandExecutor
import io.github.nayasis.kotlin.swing.app.di.Inject
import io.github.nayasis.kotlin.swing.misc.withBackground
import io.github.nayasis.kotlin.swing.window.Dialog
import io.github.nayasis.kotlin.swing.window.Windows
import io.github.nayasis.kotlin.swing.window.progress.ProgressDialog
import io.github.nayasis.simplelauncher.common.Context.Companion.linkService
import io.github.nayasis.simplelauncher.common.Context.Companion.main
import io.github.nayasis.simplelauncher.model.Link
import io.github.nayasis.simplelauncher.view.Terminal
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.swing.Swing
import java.awt.Insets
import java.io.File
import java.time.LocalDateTime
import javax.swing.JButton

private val logger = KotlinLogging.logger{}

private data class ProgressQueueContext(
    val queueButton: JButton,
    val control: ProgressQueueControl,
    val popOver: ProgressQueuePopOver,
)

/**
 * runs links.
 *
 * every run is a coroutine on the event dispatch thread. waiting for a process or a terminal suspends it
 * instead of blocking a thread, so files dropped on a link run one after another while the UI stays live.
 */
@Inject
class LinkExecutor{

    private val supervisor = SupervisorJob()

    private val scope = CoroutineScope(supervisor + Dispatchers.Swing + CoroutineExceptionHandler { _, e ->
        logger.error(e)
        Dialog.error(e.message, e)
    })

    private val childWindows = LauncherChildWindows()

    /**
     * run [link], once per file of [files] when it executes each file separately.
     *
     * @return the run, active until every command it waits for is done
     */
    fun run(link: Link, files: Collection<File>? = null): Job = scope.launch {

        linkService.save( link.apply { executedAt = LocalDateTime.now() })

        when {
            files.isNullOrEmpty() -> run(LinkCommand(link))
            files.size == 1       -> run(LinkCommand(link, files.first()))
            link.executeEach      -> runQueue(link, files)
            else                  -> run(LinkCommand(link, files))
        }

        linkService.save( link.apply { executeCount++ })

    }

    fun hideChildWindows() = childWindows.hide()

    fun restoreChildWindows() = childWindows.restore()

    /** whether a run still waits for a command, or a progress dialog is open. */
    fun hasRunningWork(): Boolean {
        return childWindows.hasProgressDialogs() || supervisor.children.any { it.isActive }
    }

    private suspend fun runQueue(link: Link, files: Collection<File>) {
        val queue = ProgressFileQueue(files)
        lateinit var dialog: ProgressDialog
        val context = createProgressQueueContext(queue) {
            updateProgress(dialog, queue, queue.currentItem())
            childWindows.refreshProgressQueue(dialog)
        }
        dialog = createProgressDialog(link.title, context.queueButton)
        childWindows.register(dialog, context.popOver)
        dialog.showAt(childWindows.progressDialogPosition(dialog))
        try {
            while (queue.hasNext()) {
                val item = queue.next() ?: continue
                updateProgress(dialog, queue, item)
                run(LinkCommand(link, item.file), wait = true, onExecutorChanged = context.control::updateExecutor)
                queue.finish(item)
                context.control.updateExecutor(null)
                childWindows.refreshProgressQueue(dialog)
            }
        } finally {
            dialog.close()
            childWindows.unregister(dialog)
        }
    }

    private fun createProgressDialog(title: String?, headerButton: JButton): ProgressDialog {
        return ProgressDialog(title).apply {
            initOwner(Windows.focusedWindow)
            addHeaderRight(headerButton)
        }
    }

    private fun createProgressQueueContext(queue: ProgressFileQueue, onPendingRemoved: () -> Unit): ProgressQueueContext {
        lateinit var popOver: ProgressQueuePopOver
        val control = ProgressQueueControl(queue) {
            popOver.refreshIfShowing()
        }
        val queueButton = JButton("☰").apply {
            toolTipText = "label.work.queue.tooltip".message()
            margin = Insets(0, 4, 0, 4)
            isFocusable = false
            font = font.deriveFont(font.size2D * 0.85f)
            addActionListener { popOver.toggle(this) }
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

    private suspend fun run(link: LinkCommand, wait: Boolean = false, onExecutorChanged: ((CommandExecutor?) -> Unit)? = null) {

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

    private suspend fun executeSequential(commands: Collection<Command>, showConsole: Boolean, onExecutorChanged: ((CommandExecutor?) -> Unit)? = null) {
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

    private suspend fun execute(command: Command, wait: Boolean, showConsole: Boolean, keepTerminalOpen: Boolean, onExecutorChanged: ((CommandExecutor?) -> Unit)? = null) {
        if( command.isEmpty() ) return
        main.printCommand("$command")
        if(showConsole) {
            runInTerminal(command, keepTerminalOpen)
        } else {
            runInBackground(command, wait, onExecutorChanged)
        }
    }

    private suspend fun runInBackground(command: Command, wait: Boolean, onExecutorChanged: ((CommandExecutor?) -> Unit)? = null) {
        logger.debug { "- command: $command" }
        val executor = try {
            withBackground { command.run() }
        } catch (e: Exception) {
            throw RuntimeException("msg.error.runtime".message().format("$command"), e)
        }
        if(!wait) return
        onExecutorChanged?.invoke(executor)
        try {
            runInterruptible(Dispatchers.IO) { executor.waitFor() }
        } finally {
            onExecutorChanged?.invoke(null)
        }
    }

    /**
     * show [command] in a terminal window and suspend until it ends.
     *
     * @param keepOpen leave the window open afterwards, so the output can be read
     */
    private suspend fun runInTerminal(command: Command, keepOpen: Boolean) {
        logger.debug { "- command: $command" }
        val terminal = Terminal()
        childWindows.register(terminal)
        childWindows.show(terminal)
        try {
            terminal.run(command)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error(e)
            Dialog.error(e)
        }
        if(!keepOpen) {
            terminal.close()
            childWindows.unregister(terminal)
        }
    }

}
