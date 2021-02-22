package com.github.nayasis.sample.treeview

import javafx.application.Application
import tornadofx.App
import tornadofx.importStylesheet

class TreeViewApp: App() {
    override val primaryView = DemoViews::class
    init {
        importStylesheet(Styles::class)
    }
}

fun main(args: Array<String>) {
    Application.launch(TreeViewApp::class.java, *args)
}