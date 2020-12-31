package com.github.nayasis.kotlin.javafx.stage

import java.awt.GraphicsConfiguration
import kotlin.math.abs
import kotlin.math.max

class BoundaryChecker {
}


data class Screen(
    val x: Axis = Axis(),
    val y: Axis = Axis()
) {
    constructor(config: GraphicsConfiguration ): this() {

    }
}

data class Axis(
    val from: Double = 0.0,
    val to: Double = 0.0,
) {
    fun within( value: Double ): Boolean {
        return value in from..to
    }

    fun length(): Double = abs( to - from )

    fun maxlength( invalue: Double ): Double {
        return when {
            within(invalue) -> 0.0
            else -> max( abs(invalue - from), abs(invalue - to) )
        }
    }
}
