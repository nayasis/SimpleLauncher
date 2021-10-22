// Copyright 2020 Kalkidan Betre Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.nayasis.sample.swing.decoratedUi.usercontrols

import java.awt.Dimension
import java.awt.Graphics
import java.awt.Image
import javax.swing.JPanel

class IconJPanel(image: Image?): JPanel() {
    private val image: Image? = null
    public override fun paintComponent(g: Graphics) {
        if (image != null) g.drawImage(image, 0, 0, null)
    }

    init {
        if (image != null) {
            this.image = image
            val size = Dimension(image.getWidth(null), image.getHeight(null))
            preferredSize = size
            minimumSize = size
            maximumSize = size
            setSize(size)
            layout = null
        }
    }
}