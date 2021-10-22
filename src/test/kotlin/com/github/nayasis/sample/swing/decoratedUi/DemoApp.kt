// Copyright 2020 Kalkidan Betre Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.nayasis.sample.swing.decoratedUi

import com.beranabyte.ui.customjframe.CustomJFrame
import java.awt.Dimension
import java.lang.Exception

object DemoApp {
    @JvmStatic
    fun main(args: Array<String>) {
        SwingUtilities.invokeLater(Runnable {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val darkTheme: Theme = DarkTheme()
            val frame = CustomJFrame(darkTheme, "Custom Decorated Window", WindowFrameType.NORMAL)
            frame.setMinimumSize(Dimension(600, 400))
            frame.setLocationRelativeTo(null)
            frame.setVisible(true)
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
            val imageIcon = ImageIcon("resources/appicon.png")
            frame.setIcon(imageIcon.getImage())

//            frame.addJFrameCloseEventAdapter(new MouseAdapter() {
//               @Override
//               public void mouseClicked(MouseEvent e) {
//                  int response = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit the app ?");
//                  if (response == JOptionPane.OK_OPTION) {
//                         System.exit(0);
//                  }
//               }
//            });
        })
    }
}