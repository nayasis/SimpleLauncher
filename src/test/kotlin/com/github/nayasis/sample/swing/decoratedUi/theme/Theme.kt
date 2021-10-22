// Copyright 2020 Kalkidan Betre Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.nayasis.sample.swing.decoratedUi.theme

import java.awt.Color

interface Theme {
    val frameBorderColor: Color
    val defaultBackgroundColor: Color
    val defaultForegroundColor: Color
    val lightForegroundColor: Color
    val defaultButtonHoverColor: Color
    val defaultButtonPressedColor: Color
    val closeButtonHoverColor: Color
    val closeButtonPressedColor: Color
}