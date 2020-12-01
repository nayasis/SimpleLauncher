package com.github.nayasis.helloworld

import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.text.FontWeight
import tornadofx.*

class HelloWorldApp : App( HelloWorld::class, Styles::class )

fun main( args:Array<String> ) {
    launch<HelloWorldApp>( args )
}


class HelloWorld : View() {

    var count = SimpleIntegerProperty(0)

    override val root = vbox {
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
            fontFamily = "SanSerif"
        }
        label {
//            fontSize = 20.px
            padding = box( 5.px )
            fontWeight = FontWeight.BOLD
            backgroundColor += c("#cecece")
        }
    }
}