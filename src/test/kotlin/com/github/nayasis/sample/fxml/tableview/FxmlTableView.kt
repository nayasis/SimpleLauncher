package com.github.nayasis.sample.fxml.tableview

import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.util.Callback
import tornadofx.*
import kotlin.reflect.KProperty1

class DemoTableView: View() {

    override val root: AnchorPane by fxml("/views/TableView.fxml")

    private val persons = listOf(
        Person("nayasis", 45),
        Person("jake", 9),
    ).asObservable()

    val main: TableView<Person> by fxid()

    init {

        main.columns.clear()

//        val colName = TableColumn<Person,String>("name").apply {
////            cellValueFactory = PropertyValueFactory("name")
//            cellValueFactory = Callback { observable(it.value, Person::name) }
//        }

        val colName = TableColumn<Person,String>("name").bindVal(Person::name)


        val colAge = TableColumn<Person,Int>("age").apply {
//            cellValueFactory = PropertyValueFactory("age")
            cellValueFactory = Callback { observable(it.value, Person::age) }
        }

        main.columns.addAll( colName, colAge )

        main.items.addAll( persons )

//        tableview( persons ) {
//            readonlyColumn("Name", Person::name )
//            readonlyColumn("Age",  Person::age )
//        }.replaceWith( tableMain )

    }

}

data class Person (
    val name: String? = null,
    val age: Int? = null,
)

fun <S,T> TableColumn<S,T>.bindVal(prop: KProperty1<S,T?>): TableColumn<S,T> {
    this.cellValueFactory = Callback { observable(it.value, prop) }
    return this
}

class DemoApp: App(DemoTableView::class)

fun main(args: Array<String> ) {
    launch<DemoApp>( args )
}

