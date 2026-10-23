package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.basica.exec.Command
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
import java.lang.ProcessBuilder.Redirect
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
                run(LinkCommand(link, item.file), wait = true, onProcessChanged = context.control::updateProcess)
                queue.finish(item)
                context.control.updateProcess(null)
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

    private suspend fun run(link: LinkCommand, wait: Boolean = false, onProcessChanged: ((Process?) -> Unit)? = null) {

        val nextCommands = link.nextCommands()

        executeSequential(
            commands = link.prevCommands(),
            showConsole = link.showConsole,
            onProcessChanged = onProcessChanged,
        )

        execute(
            command = link.toCommand(),
            wait = wait || nextCommands.isNotEmpty(),
            showConsole = link.showConsole,
            keepTerminalOpen = link.showConsole && !wait,
            onProcessChanged = onProcessChanged,
        )

        executeSequential(
            commands = nextCommands,
            showConsole = link.showConsole,
            onProcessChanged = onProcessChanged,
        )

    }

    private suspend fun executeSequential(commands: Collection<Command>, showConsole: Boolean, onProcessChanged: ((Process?) -> Unit)? = null) {
        commands.forEach { command ->
            execute(
                command = command,
                wait = true,
                showConsole = showConsole,
                keepTerminalOpen = false,
                onProcessChanged = onProcessChanged,
            )
        }
    }

    private suspend fun execute(command: Command, wait: Boolean, showConsole: Boolean, keepTerminalOpen: Boolean, onProcessChanged: ((Process?) -> Unit)? = null) {
        if( command.isEmpty() ) return
        main.printCommand("$command")
        if(showConsole) {
            runInTerminal(command, keepTerminalOpen)
        } else {
            runInBackground(command, wait, onProcessChanged)
        }
    }

    private suspend fun runInBackground(command: Command, wait: Boolean, onProcessChanged: ((Process?) -> Unit)? = null) {
        logger.debug { "- command: $command" }
        val process = try {
            withBackground { command.start() }
        } catch (e: Exception) {
            throw RuntimeException("msg.error.runtime".message().format("$command"), e)
        }
        if(!wait) return
        onProcessChanged?.invoke(process)
        try {
            runInterruptible(Dispatchers.IO) { process.waitFor() }
        } catch (e: CancellationException) {
            process.destroyForcibly()
            throw e
        } finally {
            onProcessChanged?.invoke(null)
        }
    }

    /**
     * start [this] without a console.
     *
     * the output goes where the launcher's own goes instead of into a pipe: nobody reads a pipe, so a program
     * printing more than its buffer would block, and a program started from it would keep it open.
     */
    private fun Command.start(): Process {
        return ProcessBuilder(command).apply {
            environment().putAll(this@start.environment)
            workingDirectory?.let(::File)?.takeIf { it.isDirectory }?.let { directory(it) }
            redirectInput(Redirect.INHERIT)
            redirectOutput(Redirect.INHERIT)
            redirectError(Redirect.INHERIT)
        }.start()
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
        var ended = false
        try {
            terminal.run(command)
            ended = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // stops the run like a failure in the background does, so the next commands do not run
            throw RuntimeException("msg.error.runtime".message().format("$command"), e)
        } finally {
            // a failed or cancelled run ends its command: closing the window kills the process
            if(!ended || !keepOpen) {
                terminal.close()
                childWindows.unregister(terminal)
            }
        }
    }

}
