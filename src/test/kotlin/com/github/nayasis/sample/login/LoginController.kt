package com.github.nayasis.sample.login

import tornadofx.Controller
import tornadofx.runLater

const val USERNAME = "username"
const val PASSWORD = "password"

class LoginController: Controller() {

    val loginScreen: LoginScreen by inject()
    val secureScreen: SecureScreen by inject()

    fun init() {
        with(config) {
            if(containsKey(USERNAME) && containsKey(PASSWORD)) {
                tryLogin(string(USERNAME)!!, string(PASSWORD)!!, true)
            } else {
                showLoginScreen("Please log in")
            }
        }
    }

    fun logout() {
        with(config) {
            remove(USERNAME)
            remove(PASSWORD)
            save()
        }
        showLoginScreen("Log in as another user")
    }

    fun tryLogin(username: String, password: String, remember: Boolean) {
        runAsync {
            username == "admin" && password == "secret"
        } ui { success ->
            if( success ) {
                loginScreen.clear()
                if(remember) {
                    with(config) {
                        set(USERNAME to username)
                        set(PASSWORD to password)
                        save()
                    }
                }
                showSecureScreen()
            } else {
                showLoginScreen("Login failed. Please try again.", true)
            }

        }
    }

    private fun showSecureScreen() {
        loginScreen.replaceWith(secureScreen,sizeToScene = true, centerOnScreen = true)
    }

    private fun showLoginScreen(message: String, shake: Boolean = false) {
        secureScreen.replaceWith(loginScreen,sizeToScene = true, centerOnScreen = true)
        runLater {
            if(shake)
                loginScreen.shake()
        }
    }

}