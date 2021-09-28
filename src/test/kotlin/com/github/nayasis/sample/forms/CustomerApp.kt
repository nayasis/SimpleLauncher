package com.github.nayasis.sample.forms

import javafx.application.Application
import tornadofx.App

fun main(args: Array<String> ) {
    Application.launch(CustomerApp::class.java, *args)
}

class CustomerApp: App(CustomerForm::class,Styles::class)
