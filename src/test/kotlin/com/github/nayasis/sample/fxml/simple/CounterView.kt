package com.github.nayasis.sample.fxml.simple

import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.BorderPane
import tornadofx.*
import java.util.*

class CounterView: View() {

    init{
        messages = ResourceBundle.getBundle( "CounterView")
    }

    override val root: BorderPane by fxml("/views/CounterView.fxml")

    val persons = listOf(
        Person("nayasis", 45),
        Person("jake", 9),
    ).asObservable()

    val counter = SimpleIntegerProperty()
    val counterLabel: Label by fxid()
    val tableMain: TableView<Person> by fxid()

    init {

        counterLabel.bind( counter )

        tableMain.columns.clear()

        val colName = TableColumn<Person,String>("name").apply {
            cellValueFactory = PropertyValueFactory("name")
//            cellValueFactory = Callback { observable(it.value, Person::name) }
        }

        val colAge = TableColumn<Person,Int>("age").apply {
            cellValueFactory = PropertyValueFactory("age")
//            cellValueFactory = Callback { observable(it.value, Person::age) }
        }

        tableMain.columns.addAll( colName, colAge )

        tableMain.items.addAll( persons )

        tableview( persons ) {
            readonlyColumn("Name", Person::name )
            readonlyColumn("Age",  Person::age )
        }.replaceWith( tableMain )

    }

    fun increment() {

        counter.value += 1
    }


}

data class Person (
    val name: String? = null,
    val age: Int? = null,
)

class CounterApp: App(CounterView::class) {
    init {
        FX.locale = Locale.ENGLISH
    }

}

fun main(args: Array<String> ) {
    launch<CounterApp>( args )
}

