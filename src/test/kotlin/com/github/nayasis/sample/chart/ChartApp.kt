package com.github.nayasis.sample.chart

import javafx.application.Application
import tornadofx.App

fun main(args: Array<String>) {
    Application.launch(ChartApp::class.java, *args)
}

class ChartApp: App() {
    override val primaryView = ChartView::class
}