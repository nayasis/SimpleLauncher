package io.github.nayasis.simplelauncher.view

import com.jediterm.core.util.TermSize
import com.jediterm.terminal.ProcessTtyConnector
import com.jediterm.terminal.model.hyperlinks.HyperlinkFilter
import com.jediterm.terminal.model.hyperlinks.LinkInfo
import com.jediterm.terminal.model.hyperlinks.LinkResult
import com.jediterm.terminal.model.hyperlinks.LinkResultItem
import com.jediterm.terminal.ui.JediTermWidget
import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.pty4j.WinSize
import io.github.nayasis.kotlin.basica.core.string.toPath
import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.kotlin.swing.misc.Desktop
import io.github.nayasis.kotlin.swing.misc.withBackground
import io.github.nayasis.kotlin.swing.property.InsetProperty
import io.github.nayasis.kotlin.swing.property.WindowProperty
import io.github.nayasis.kotlin.swing.window.loadDefaultIcon
import io.github.nayasis.kotlin.swing.window.watchMaximized
import io.github.nayasis.simplelauncher.common.Context.Companion.config
import io.github.nayasis.simplelauncher.view.theme.BlackTerminalTheme
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.swing.JFrame
import kotlin.io.path.exists

private val logger = KotlinLogging.logger {}

/**
 * window running a command in a pseudo terminal.
 *
 * the window keeps its bounds across runs. closing it ends the command.
 */
class Terminal: JFrame() {

    private var widget: JediTermWidget? = null

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        contentPane.layout = BorderLayout()
        minimumSize = Dimension(100, 100)
        loadDefaultIcon()

        config.windowTerminal?.sanitizeTerminalWindowProperty()?.bind(this, includeChildren = false) ?: run {
            setSize(700, 600)
            setLocationRelativeTo(null)
        }
        watchMaximized()

        addWindowListener(object: WindowAdapter() {
            override fun windowClosing(e: WindowEvent) = persistWindowState()
            override fun windowClosed(e: WindowEvent) = disconnect()
        })
    }

    /**
     * run [command] and suspend until it ends. the window stays open, titled as done.
     */
    suspend fun run(command: Command) {
        title = "$command"
        val process = withBackground { toPtyProcess(command) }
        val widget = JediTermWidget(80, 200, BlackTerminalTheme()).apply {
            ttyConnector = PtyProcessTtyConnector(process, StandardCharsets.UTF_8, command.command)
            addHyperlinkFilter(UrlHyperlinkFilter)
            addListener { this.close() }
            start()
        }
        this.widget = widget
        contentPane.add(widget, BorderLayout.CENTER)
        contentPane.revalidate()
        widget.requestFocusInWindow()
        runInterruptible(Dispatchers.IO) { process.waitFor() }
        title = "Done - $title"
    }

    /** save the window state and close it, ending the command if it still runs. */
    fun close() {
        if (!isDisplayable) return
        persistWindowState()
        dispose()
    }

    private fun disconnect() {
        widget?.let { widget ->
            runCatching { widget.ttyConnector?.close() }.onFailure { e -> logger.error(e) }
            runCatching { widget.close() }.onFailure { e -> logger.error(e) }
        }
        widget = null
    }

    private fun toPtyProcess(cmd: Command): PtyProcess {
        val envs = System.getenv().toMutableMap().apply {
            put("TERM", "xterm-256color")
        }
        return PtyProcessBuilder()
            .setCommand(cmd.command.toTypedArray())
            .setEnvironment(envs)
            .also { builder ->
                if(cmd.workingDirectory?.toPath()?.exists() == true) {
                    builder.setDirectory(cmd.workingDirectory)
                }
            }
            .start()
    }

    private fun persistWindowState() {
        runCatching {
            config.windowTerminal = WindowProperty(this, includeChildren = false)
            config.save()
        }.onFailure { e -> logger.error(e) }
    }

}

/** connects JediTerm to a pty4j process, passing window resizes on to the pseudo terminal. */
private class PtyProcessTtyConnector(
    private val process: PtyProcess,
    charset: Charset,
    commandLine: List<String>,
): ProcessTtyConnector(process, charset, commandLine) {

    override fun getName(): String = "Local"

    override fun resize(termSize: TermSize) {
        if (isConnected) process.winSize = WinSize(termSize.columns, termSize.rows)
    }

}

/** turns http(s) URLs in the output into links that open in the browser. */
private object UrlHyperlinkFilter: HyperlinkFilter {

    private val URL = "https?://[^\\s\"'<>()\\[\\]]+".toRegex()

    override fun apply(line: String): LinkResult? {
        val items = URL.findAll(line).map { match ->
            LinkResultItem(match.range.first, match.range.last + 1, LinkInfo {
                runCatching { Desktop.browse(match.value) }.onFailure { logger.error(it) }
            })
        }.toList()
        return if (items.isEmpty()) null else LinkResult(items)
    }

}

/**
 * drop a maximized state that has no normal bounds to go back to: they were never captured,
 * so un-maximizing would land on the default bounds.
 */
internal fun WindowProperty.sanitizeTerminalWindowProperty(): WindowProperty {
    if(maximized && previousBoundary?.maximized == true && previousBoundary?.boundary?.isDefaultBoundary() == true) {
        maximized = false
        previousBoundary?.maximized = false
    }
    return this
}

private fun InsetProperty.isDefaultBoundary(): Boolean {
    return x == 100 && y == 100 && width == 500 && height == 600
}
