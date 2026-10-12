package io.github.nayasis.simplelauncher.view.terminal

import com.formdev.flatlaf.FlatIntelliJLaf
import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.kotlin.swing.misc.launchUi
import io.github.nayasis.simplelauncher.view.Terminal
import javax.swing.SwingUtilities

/**
 * manual demo: runs a command in the terminal window. it asserts nothing.
 *
 * pass the command as arguments, e.g. `cmd /c dir`.
 */
fun main(args: Array<String>) {
    val command = args.joinToString(" ").ifBlank { "cmd /c echo hello from the terminal && dir" }
    SwingUtilities.invokeAndWait { FlatIntelliJLaf.setup() }
    launchUi {
        val terminal = Terminal()
        terminal.isVisible = true
        terminal.run(Command(command))
        println("terminal finished: ${terminal.title}")
    }
}
