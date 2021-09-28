package com.github.nayasis.kotlin.javafx.geometry

import javafx.geometry.Insets

fun Insets(top: Number, right: Number, bottom: Number, left: Number): Insets =
    Insets(top.toDouble(), right.toDouble(), bottom.toDouble(), left.toDouble())