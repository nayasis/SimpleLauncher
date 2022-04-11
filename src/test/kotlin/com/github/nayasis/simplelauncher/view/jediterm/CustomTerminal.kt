package com.github.nayasis.simplelauncher.view.jediterm

import com.formdev.flatlaf.ui.FlatScrollBarUI
import com.jediterm.terminal.model.JediTerminal
import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.TerminalPanel
import com.jediterm.terminal.ui.settings.SettingsProvider
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.SystemColor.scrollbar
import javax.swing.JComponent
import javax.swing.JScrollBar


class CustomTerminal: JediTermWidget {

    constructor(setting: SettingProvider): super(80,24,setting)
    constructor(columns: Int, lines: Int, setting: SettingProvider): super(columns, lines, setting)
    constructor(dimension: Dimension, setting: SettingProvider): super(dimension.width, dimension.height, setting)

    override fun createScrollBar(): JScrollBar? {
        val scrollBar = JScrollBar()
//        scrollBar.setUI(ScrollBarUIImpl(scrollBar,myTerminalPanel,mySettingsProvider))
//        scrollBar.setUI(CustomScrollBarUI())
        return scrollBar
//        return null
    }

    override fun getTerminal(): JediTerminal? {
        return myTerminal
    }

    class ScrollBarUIImpl(
        private val bar: JScrollBar,
        private val myTerminalPanel: TerminalPanel,
        private val mySettingsProvider: SettingsProvider
    ): FlatScrollBarUI() {

        override fun paint(g: Graphics, c: JComponent) {
            super.paint(g, c)
            val result = myTerminalPanel.findResult
            if (result != null) {
                val modelHeight = bar.model.maximum - bar.model.minimum
                val anchorHeight = Math.max(2, c.height / modelHeight)
                val color: Color = mySettingsProvider.terminalColorPalette
                    .getBackground(mySettingsProvider.foundPatternColor.background!!)
                g.color = color
                for (r in result.items) {
                    val where = c.height * r.start.y / modelHeight
                    g.fillRect(c.x, c.y + where, c.width, anchorHeight)
                }
            }
        }

//        override fun paintTrack(g: Graphics, c: JComponent, trackBounds: Rectangle) {
//            super.paintTrack(g, c, trackBounds)
//            val result = myTerminalPanel.findResult
//            if (result != null) {
//                val modelHeight = scrollbar.model.maximum - scrollbar.model.minimum
//                val anchorHeight = Math.max(2, trackBounds.height / modelHeight)
//                val color = mySettingsProvider.terminalColorPalette
//                    .getBackground(mySettingsProvider.foundPatternColor.background!!)
//                g.color = color
//                for (r in result.items) {
//                    val where = trackBounds.height * r.start.y / modelHeight
//                    println( ">> modelHeight: $modelHeight, anchorHeight: $anchorHeight, where: $where")
//                    g.fillRect(trackBounds.x, trackBounds.y + where, trackBounds.width, anchorHeight)
//                }
//            }
//        }
//
//        override fun paint(g: Graphics, c: JComponent) {
//            paintTrack(g,c,trackBounds)
//            println(">> on paint?? :${trackBounds}")
//            val thumbBounds = thumbBounds
//            if (thumbBounds.intersects(g!!.clipBounds)) {
//                paintThumb(g, c, thumbBounds)
//            }
//        }
    }


}