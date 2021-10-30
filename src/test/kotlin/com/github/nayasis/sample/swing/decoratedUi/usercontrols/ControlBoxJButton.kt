// Copyright 2020 Kalkidan Betre Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.nayasis.sample.swing.decoratedUi.usercontrols

import com.github.nayasis.kotlin.awt.drawLine
import com.github.nayasis.kotlin.awt.drawRect
import com.github.nayasis.sample.swing.decoratedUi.theme.DarkTheme
import com.github.nayasis.sample.swing.decoratedUi.theme.Theme
import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.Point
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton

class ControlBoxJButton(var buttonType: ButtonType, private val theme: Theme = DarkTheme()): JButton(), MouseListener {

    override fun getInsets(): Insets = Insets(0, 0, 0, 0)

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = (g.create() as Graphics2D).let {
            it.color = background
            it.fillRect(0, 0, width, height)
            it.stroke = BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            it.color = theme.lightForegroundColor
            it
        }
        when (buttonType) {
            ButtonType.MINIMIZE -> {
                g2d.drawLine(Point(width / 2 - 5, height / 2),Point(width / 2 + 5, height / 2) )
            }
            ButtonType.MAXIMIZE -> g2d.drawRect(Point(width / 2 - 5, height / 2 - 5), 10, 10)
            ButtonType.RESTORE -> {
                g2d.drawRect(Point(width / 2 - 5, height / 2 - 3), 8, 8)
                val p = Point(width / 2 + 5, height / 2 - 5)
                g2d.drawLine(p.x, p.y, p.x - 8, p.y)
                g2d.drawLine(p.x, p.y, p.x, p.y + 8)
                g2d.drawLine(p.x - 8, p.y, p.x - 8, p.y + 2)
                g2d.drawLine(p.x, p.y + 8, p.x - 2, p.y + 8)
            }
            ButtonType.CLOSE -> {
                g2d.drawLine(Point(width / 2 - 5, height / 2 - 5), Point(width / 2 + 5, height / 2 + 5))
                g2d.drawLine(Point(width / 2 + 5, height / 2 - 5), Point(width / 2 - 5, height / 2 + 5))
            }
        }
        g2d.dispose()
    }

    override fun mouseClicked(e: MouseEvent?) {}
    override fun mousePressed(e: MouseEvent) {
        background = if (buttonType === ButtonType.CLOSE) theme.closeButtonPressedColor else theme.defaultButtonHoverColor
    }

    override fun mouseReleased(e: MouseEvent?) {}
    override fun mouseEntered(e: MouseEvent) {
        background = if (buttonType == ButtonType.CLOSE) theme.closeButtonHoverColor else theme.defaultButtonHoverColor
    }

    override fun mouseExited(e: MouseEvent) {
        background = theme.defaultBackgroundColor
    }

    init {
        isOpaque = false
        addMouseListener(this)
    }

}