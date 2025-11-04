package io.github.nayasis.sample.swing.decoratedUi

import io.github.nayasis.kotlin.basica.core.klass.Classes
import io.github.nayasis.sample.swing.decoratedUi.customjframe.CustomJFrame
import io.github.nayasis.sample.swing.decoratedUi.customjframe.WindowFrameType
import io.github.nayasis.sample.swing.decoratedUi.theme.DarkTheme
import java.awt.Dimension
import javax.swing.*

object DemoApp {
    @JvmStatic
    fun main(args: Array<String>) {
        SwingUtilities.invokeLater(Runnable {

            runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }.onFailure { it.printStackTrace() }

            val frame = CustomJFrame(DarkTheme(), "Custom Decorated Window", WindowFrameType.NORMAL).apply {
                minimumSize = Dimension(600, 400)
                setLocationRelativeTo(null)
                isVisible = true
            }

            val menubar = JMenuBar().apply {
                add(JMenu("File").apply {
                    add(JMenuItem("Open"))
                    add(JMenuItem("Close"))
                })
                add(JMenu("Edit").apply {
                    add(JMenuItem("Copy"))
                })
                add(JMenu("View").apply {
                    add(JMenuItem("Tools"))
                })
            }

            frame.addUserControlsToTitleBar(menubar)

            frame.setIcon(ImageIcon(Classes.getResource("image/appicon.png")).image)

        })
    }
}