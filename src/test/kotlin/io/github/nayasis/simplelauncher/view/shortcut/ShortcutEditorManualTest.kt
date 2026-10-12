package io.github.nayasis.simplelauncher.view.shortcut

import com.formdev.flatlaf.FlatIntelliJLaf
import io.github.nayasis.kotlin.basica.model.Messages.Companion.loadMessages
import io.github.nayasis.simplelauncher.service.ShortcutSettings
import io.github.nayasis.simplelauncher.view.ShortcutEditor
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

/** manual demo: shows the shortcut settings dialog alone and prints the result. it asserts nothing. */
fun main() {
    "/message/**.prop".loadMessages()
    SwingUtilities.invokeLater {
        FlatIntelliJLaf.setup()
        val result = ShortcutEditor().showDialog(null, ShortcutSettings.load(emptyMap()))
        println(result?.toConfigMap() ?: "canceled")
        exitProcess(0)
    }
}
