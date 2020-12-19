package com.github.nayasis.kotlin.javafx.stage

import com.github.nayasis.kotlin.basica.toList
import javafx.scene.image.Image
import javafx.stage.Stage
import javafx.stage.Window


private const val DEFAULT_WIDTH  = 400
private const val DEFAULT_HEIGHT = 300

val DEFAULT_ICONS = ArrayList<Image>()

fun focusedWindow(): Window? {
    for( window in windows())
        if( window.isFocused ) return window
    return null
}

fun windows(): List<Window> {
    return Window.impl_getWindows().toList()
}

fun Stage.loadDefaultIcon() {
    if( DEFAULT_ICONS.isNotEmpty() ) {
        this.icons.addAll(DEFAULT_ICONS)
    }
}