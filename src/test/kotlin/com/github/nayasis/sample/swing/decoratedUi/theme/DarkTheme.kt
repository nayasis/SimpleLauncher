// Copyright 2020 Kalkidan Betre Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.nayasis.sample.swing.decoratedUi.theme

import java.awt.Color

class DarkTheme: Theme {
    override val frameBorderColor: Color
        get() = Color(0, 255, 255)
    override val defaultBackgroundColor: Color
        get() = Color(64, 64, 64)
    override val defaultForegroundColor: Color
        get() = Color(255, 255, 255)
    override val lightForegroundColor: Color
        get() = Color(192, 192, 192)
    override val defaultButtonHoverColor: Color
        get() = Color(101, 101, 101)
    override val defaultButtonPressedColor: Color
        get() = Color(101, 101, 101)
    override val closeButtonHoverColor: Color
        get() = Color(232, 17, 35)
    override val closeButtonPressedColor: Color
        get() = Color(241, 112, 122)
}