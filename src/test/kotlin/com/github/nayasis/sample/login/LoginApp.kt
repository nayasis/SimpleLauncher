package com.github.nayasis.sample.login

import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch

fun main(args: Array<String> ) {
    launch<LoginApp>(args)
}

class LoginApp: App( LoginScreen::class, Styles::class) {

    val loginController: LoginController by inject()

    override fun start(stage: Stage) {
        super.start(stage)
        loginController.init()
    }

}