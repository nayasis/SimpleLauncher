package com.github.nayasis.sample.tornadofx.basic

import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.text.FontWeight
import tornadofx.*

fun main( args:Array<String> ) {
    launch<HelloWorld>( args )
}

class HelloWorld: App( MainView::class, Styles::class )

class MainView: View("Hello world!") {

    var count = SimpleIntegerProperty(0)

    override val root = vbox() {
        button( "click" ) {
            action {
                count.set( count.get() + 1 )
            }
        }
        label( "Hello world" ) {
            bind( count )
        }
    }
}


class Styles : Stylesheet() {
    init {
        root {
            fontSize = 9.pt
            fontFamily = "Arial"
        }
        label {
            fontSize = 20.px
            padding = box( 5.px )
            fontWeight = FontWeight.BOLD
            backgroundColor += c("#cecece")
        }
    }
}