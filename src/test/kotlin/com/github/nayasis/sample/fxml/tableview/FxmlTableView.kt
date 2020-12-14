package com.github.nayasis.sample.fxml.tableview

import com.github.nayasis.kotlin.javafx.control.tableview.column.bindVal
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import tornadofx.*

class DemoTableView: View() {

    override val root: AnchorPane by fxml("/views/TableView.fxml")

    private val data = SortedFilteredList(listOf(
        Person("nayasis", 45),
        Person("jake", 9),
    ).asObservable())

    val main: TableView<Person> by fxid()
    val colName: TableColumn<Person,String> by fxid()
    val colAge: TableColumn<Person,Int> by fxid()

    init {
        initTable()
    }

    private fun initTable() {

//        val colName = main.findColumnBy("colName")!!.bindVal(Person::name)
//        val colAge  = main.findColumnBy("colAge")!!.bindVal(Person::age)

        colName.bindVal(Person::name)
        colAge.bindVal(Person::age)

        data.bindTo( main )

    }

}

data class Person (
    val name: String? = null,
    val age: Int? = null,
)

class DemoApp: App(DemoTableView::class)

fun main(args: Array<String> ) {
    launch<DemoApp>( args )
}

