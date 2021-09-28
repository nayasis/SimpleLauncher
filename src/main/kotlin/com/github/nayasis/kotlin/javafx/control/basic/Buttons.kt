package com.github.nayasis.kotlin.javafx.control.basic

import javafx.scene.control.Button
import javafx.scene.control.OverrunStyle
import org.controlsfx.glyphfont.FontAwesome

fun Button.create(text: String? = null, glyph: FontAwesome.Glyph? = null): Button {
    return when (glyph) {
        null -> Button(text)
        else -> Button(text,FontAwesome().create(glyph))
    }.apply { textOverrun = OverrunStyle.CLIP }
}