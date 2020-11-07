package com.github.nayasis.helloworld

import javafx.scene.text.FontWeight
import tornadofx.*

class HelloWorldApp : App( HelloWorld::class, Styles::class )

fun main( args:Array<String> ) {
    launch<HelloWorldApp>( args )
}

class Styles : Stylesheet() {
    init {
        label {
            fontSize = 20.px
            padding = box( 5.px )
            fontWeight = FontWeight.BOLD
            backgroundColor += c("#cecece")
        }
    }
}