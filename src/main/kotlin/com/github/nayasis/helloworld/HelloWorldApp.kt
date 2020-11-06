package com.github.nayasis.helloworld

import javafx.scene.text.FontWeight
import tornadofx.App
import tornadofx.Stylesheet
import tornadofx.c
import tornadofx.px

class HelloWorldApp : App( HelloWorld::class, Styles::class )

class Styles : Stylesheet() {
    init {
        label {
            fontSize = 20.px
            fontWeight = FontWeight.BOLD
            backgroundColor += c("#cecece")
        }
    }
}