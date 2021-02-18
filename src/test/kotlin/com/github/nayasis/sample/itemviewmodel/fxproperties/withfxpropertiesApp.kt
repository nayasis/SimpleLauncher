package com.github.nayasis.sample.itemviewmodel.fxproperties

import javafx.application.Application
import com.github.nayasis.sample.itemviewmodel.fxproperties.views.ItemViewModelWithFxMainView
import tornadofx.App

class WithFXPropertiesApp : App(ItemViewModelWithFxMainView::class)


fun main(args: Array<String>) {
    Application.launch(WithFXPropertiesApp::class.java, *args)
}