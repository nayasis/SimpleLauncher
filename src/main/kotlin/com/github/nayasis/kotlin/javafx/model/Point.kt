package com.github.nayasis.kotlin.javafx.model

import javafx.scene.input.MouseEvent

data class Point(
    var x: Double = 0.0,
    var y: Double = 0.0,
) {
    constructor(event: MouseEvent): this(
        x = event.sceneX,
        y = event.sceneY
    )
    constructor(x: Int, y: Int): this(
        x.toDouble(), y.toDouble()
    )
}