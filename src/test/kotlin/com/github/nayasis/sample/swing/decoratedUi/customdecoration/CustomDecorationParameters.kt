// Copyright 2020 Kalkidan Betre Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.nayasis.sample.swing.decoratedUi.customdecoration

import java.util.concurrent.atomic.AtomicInteger

object CustomDecorationParameters {

    private val _titleBarHeight                = AtomicInteger(27)
    private val _controlBoxWidth               = AtomicInteger(150)
    private val _iconWidth                     = AtomicInteger(40)
    private val _extraLeftReservedWidth        = AtomicInteger(0)
    private val _extraRightReservedWidth       = AtomicInteger(0)
    private val _maximizedWindowFrameThickness = AtomicInteger(10)
    private val _frameResizeBorderThickness    = AtomicInteger(4)
    private val _frameBorderThickness          = AtomicInteger(1)

    var titleBarHeight: Int
        get() = _titleBarHeight.get()
        set(value) = _titleBarHeight.set(value)

    var controlBoxWidth: Int
        get() = _controlBoxWidth.get()
        set(value) = _controlBoxWidth.set(value)

    var iconWidth: Int
        get() = _iconWidth.get()
        set(value) = _iconWidth.set(value)

    var extraLeftReservedWidth: Int
        get() = _extraLeftReservedWidth.get()
        set(value) = _extraLeftReservedWidth.set(value)

    var extraRightReservedWidth: Int
        get() = _extraRightReservedWidth.get()
        set(value) = _extraRightReservedWidth.set(value)

    var maximizedWindowFrameThickness: Int
        get() = _maximizedWindowFrameThickness.get()
        set(maximizedWindowFrameThickness) = _maximizedWindowFrameThickness.set(maximizedWindowFrameThickness)

    var frameResizeBorderThickness: Int
        get() = _frameResizeBorderThickness.get()
        set(value) = _frameResizeBorderThickness.set(value)

    var frameBorderThickness: Int
        get() = _frameBorderThickness.get()
        set(value) = _frameBorderThickness.set(value)

}
