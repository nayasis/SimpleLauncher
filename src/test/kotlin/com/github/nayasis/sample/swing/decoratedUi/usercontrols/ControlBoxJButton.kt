// Copyright 2020 Kalkidan Betre Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.nayasis.sample.swing.decoratedUi.usercontrols

import com.github.nayasis.sample.swing.decoratedUi.theme.Theme
import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.Point
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton

class ControlBoxJButton(var controlBoxButtonType: ButtonType, val theme: Theme): JButton(), MouseListener {

    override fun getInsets(): Insets {
        return Insets(0, 0, 0, 0)
    }

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val width = width
        val height = height
        val g2d = g.create() as Graphics2D
        g2d.color = background
        g2d.fillRect(0, 0, width, height)
        g2d.stroke = BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2d.color = theme.lightForegroundColor
        var markStPt = Point(width / 2 - 5, height / 2 - 5)
        var markEnPt = Point(width / 2 + 5, height / 2 + 5)
        when (controlBoxButtonType) {
            ButtonType.MINIMIZE -> {
                markStPt = Point(width / 2 - 5, height / 2)
                markEnPt = Point(width / 2 + 5, height / 2)
                g2d.drawLine(markStPt.x, markStPt.y, markEnPt.x, markEnPt.y)
            }
            ButtonType.MAXIMIZE -> g2d.drawRect(markStPt.x, markStPt.y, 10, 10)
            ButtonType.RESTORE -> {
                markStPt = Point(width / 2 - 5, height / 2 - 3)
                g2d.drawRect(markStPt.x, markStPt.y, 8, 8)
                markStPt = Point(width / 2 + 5, height / 2 - 5)
                g2d.drawLine(markStPt.x, markStPt.y, markStPt.x - 8, markStPt.y)
                g2d.drawLine(markStPt.x, markStPt.y, markStPt.x, markStPt.y + 8)
                g2d.drawLine(markStPt.x - 8, markStPt.y, markStPt.x - 8, markStPt.y + 2)
                g2d.drawLine(markStPt.x, markStPt.y + 8, markStPt.x - 2, markStPt.y + 8)
            }
            ButtonType.CLOSE -> {
                g2d.drawLine(markStPt.x, markStPt.y, markEnPt.x, markEnPt.y)
                markStPt = Point(width / 2 + 5, height / 2 - 5)
                markEnPt = Point(width / 2 - 5, height / 2 + 5)
                g2d.drawLine(markStPt.x, markStPt.y, markEnPt.x, markEnPt.y)
            }
        }
        g2d.dispose()
    }

    override fun mouseClicked(e: MouseEvent) {}
    override fun mousePressed(e: MouseEvent) {
        if (controlBoxButtonType === ButtonType.CLOSE) background = theme.closeButtonPressedColor else background =
            theme.defaultButtonHoverColor
    }

    override fun mouseReleased(e: MouseEvent) {}
    override fun mouseEntered(e: MouseEvent) {
        if (controlBoxButtonType === ButtonType.CLOSE) background = theme.closeButtonHoverColor else background =
            theme.defaultButtonHoverColor
    }

    override fun mouseExited(e: MouseEvent) {
        background = theme.defaultBackgroundColor
    }

    init {
        isOpaque = false
        addMouseListener(this)
    }

}