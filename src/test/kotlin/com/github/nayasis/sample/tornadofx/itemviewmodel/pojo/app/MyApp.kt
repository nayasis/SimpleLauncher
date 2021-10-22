package com.github.nayasis.sample.tornadofx.itemviewmodel.pojo.app

import javafx.application.Application
import com.github.nayasis.sample.itemviewmodel.pojo.view.itemViewModelWithPojos
import tornadofx.App

class MyApp : App(itemViewModelWithPojos::class, Styles::class)

fun main(args: Array<String>) {
    Application.launch(MyApp::class.java, *args)
}
