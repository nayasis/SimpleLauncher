package com.github.nayasis.kotlin.javafx.stage

import com.github.nayasis.kotlin.basica.toList
import javafx.scene.image.Image
import javafx.stage.Stage
import javafx.stage.Window

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

