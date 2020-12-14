package com.github.nayasis.sample.fxml

import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import tornadofx.App
import tornadofx.FX
import tornadofx.View
import tornadofx.bind
import tornadofx.launch
import java.util.*

class CounterView: View() {

    init{
        messages = ResourceBundle.getBundle( "CounterView")
    }

    override val root: BorderPane by fxml("/views/CounterView.fxml")

    val counter = SimpleIntegerProperty()
    val counterLabel: Label by fxid()

    init {
        counterLabel.bind( counter )
    }

    fun increment() {
        counter.value += 1
    }

}

class CounterApp: App(CounterView::class) {
    init {
        FX.locale = Locale.ENGLISH
    }

}

fun main(args: Array<String> ) {
    launch<CounterApp>( args )
}