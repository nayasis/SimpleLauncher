package com.github.nayasis.sample.login

import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.text.Font
import javafx.scene.text.Font.font
import tornadofx.Stylesheet.Companion.button
import tornadofx.Stylesheet.Companion.label
import tornadofx.View
import tornadofx.action
import tornadofx.borderpane
import tornadofx.button
import tornadofx.center
import tornadofx.hbox
import tornadofx.label
import tornadofx.top
import tornadofx.vbox


class SecureScreen: View("Secure Screen") {

    val loginController: LoginController by inject()

    override val root = borderpane{

        setPrefSize(800.0, 600.0)

        top {
            label(title) {
                font = font(22.0)
            }
        }

        center {
            vbox(spacing=15) {
                alignment = Pos.CENTER
                label("If you can see this, you are successfully logged in!")
                hbox {
                   alignment = Pos.CENTER
                   button("Logout") {
                       action {
                           loginController.logout()
                       }
                   }
                   button("Exit") {
                       action {
                           Platform.exit()
                       }
                   }
                }
            }
        }

    }


}