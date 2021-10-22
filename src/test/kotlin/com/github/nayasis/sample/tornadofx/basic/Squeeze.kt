package com.github.nayasis.sample.tornadofx.basic

import tornadofx.*

class Squeeze : View() {
    override val root = squeezebox {
        fold( "Custom Editor", expanded = true) {
            form {
                fieldset("Customer Details") {
                    field("Name") { textfield() }
                    field("Password") { textfield() }
                }
            }
        }
        fold( "Some other editor", expanded = true) {
            stackpane {
                label("Nothing here")
            }
        }
    }
}

class SqueezeApp: App( Squeeze::class )

fun main() {
    launch<SqueezeApp>()
}