package com.github.nayasis.sample.swing.decoratedUi

import com.github.nayasis.sample.swing.decoratedUi.customjframe.CustomJFrame
import com.github.nayasis.sample.swing.decoratedUi.customjframe.WindowFrameType
import com.github.nayasis.sample.swing.decoratedUi.theme.DarkTheme
import java.awt.Dimension
import javax.swing.ImageIcon
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.SwingUtilities
import javax.swing.UIManager

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

            val jMenuBar = JMenuBar()
            val fileMenu = JMenu("File")
            val editMenu = JMenu("Edit")
            val viewMenu = JMenu("View")

            val openMenu = JMenuItem("Open")
            val closeMenu = JMenuItem("Close")
            fileMenu.add(openMenu)
            fileMenu.add(closeMenu)

            val copyMenu = JMenuItem("Copy")
            editMenu.add(copyMenu)

            val toolsMenu = JMenuItem("Tools")
            viewMenu.add(toolsMenu)

            jMenuBar.add(fileMenu)
            jMenuBar.add(editMenu)
            jMenuBar.add(viewMenu)
            frame.addUserControlsToTitleBar(jMenuBar)

            val imageIcon = ImageIcon("resources/image/appicon.png")
            frame.setIcon(imageIcon.getImage())

        })
    }
}